package com.fptuni.vms.service.impl;

import com.fptuni.vms.repository.ReportRepository;
import com.fptuni.vms.service.ReportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Override
    public Map<String, Object> getUserRegistrationStats(String rangeType, LocalDate from, LocalDate to, String sort) {
        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now();

        List<Object[]> rawData = reportRepository.countUsersByDateRange(from, to);
        Map<String, Long> grouped = new LinkedHashMap<>();

        if ("week".equalsIgnoreCase(rangeType)) {
            WeekFields wf = WeekFields.ISO;
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = "Tuần " + date.get(wf.weekOfMonth()) + " (" + date.getMonthValue() + "/" + date.getYear() + ")";
                grouped.merge(label, count, Long::sum);
            });
        } else if ("month".equalsIgnoreCase(rangeType)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = date.getMonthValue() + "/" + date.getYear();
                grouped.merge(label, count, Long::sum);
            });
        } else if ("year".equalsIgnoreCase(rangeType)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = String.valueOf(date.getYear());
                grouped.merge(label, count, Long::sum);
            });
        }

        List<String> labels = new ArrayList<>(grouped.keySet());
        if ("desc".equalsIgnoreCase(sort)) Collections.reverse(labels);
        List<Long> counts = labels.stream().map(grouped::get).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("counts", counts);
        return result;
    }

    @Override
    public Map<String, Long> getUserRoleDistribution() {
        return reportRepository.countUsersByRole();
    }

    @Override
    public Map<String, Object> getDrillDownStats(String rangeType, String label) {
        LocalDate from, to;

        try {
            if ("year".equalsIgnoreCase(rangeType)) {
                // label ví dụ: "5/2025"
                String[] parts = label.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                from = LocalDate.of(year, month, 1);
                to = from.plusMonths(1).minusDays(1);
                return getUserRegistrationStats("month", from, to, "asc");
            } else if ("month".equalsIgnoreCase(rangeType)) {
                // label ví dụ: "Tuần 2 (10/2025)"
                String[] parts = label.replace("Tuần ", "").replace("(", "").replace(")", "").split(" ");
                int weekNum = Integer.parseInt(parts[0]);
                String[] ym = parts[1].split("/");
                int month = Integer.parseInt(ym[0]);
                int year = Integer.parseInt(ym[1]);
                from = LocalDate.of(year, month, 1).plusWeeks(weekNum - 1);
                to = from.plusWeeks(1).minusDays(1);
                return getUserRegistrationStats("week", from, to, "asc");
            } else if ("week".equalsIgnoreCase(rangeType)) {
                // Nếu là tuần → hiển thị theo ngày chi tiết
                from = LocalDate.now().minusDays(6);
                to = LocalDate.now();
                return getUserRegistrationStats("week", from, to, "asc");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("labels", List.of(), "counts", List.of(), "rangeType", rangeType);
    }

    @Override
    public void exportReportToExcel(String type, String rangeType, LocalDate from, LocalDate to, OutputStream out) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            if ("user".equalsIgnoreCase(type)) {
                // --- Thống kê người dùng đăng ký ---
                Map<String, Object> stats = getUserRegistrationStats(rangeType, from, to, "asc");
                List<String> labels = (List<String>) stats.get("labels");
                List<Long> counts = (List<Long>) stats.get("counts");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Thời gian");
                header.createCell(1).setCellValue("Số lượng người dùng");

                for (int i = 0; i < labels.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(labels.get(i));
                    row.createCell(1).setCellValue(counts.get(i));
                }

                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);

            } else if ("role".equalsIgnoreCase(type)) {
                // --- Thống kê người dùng theo vai trò ---
                Map<String, Long> roles = getUserRoleDistribution();

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Vai trò");
                header.createCell(1).setCellValue("Số lượng");

                int rowIdx = 1;
                for (Map.Entry<String, Long> e : roles.entrySet()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(e.getKey());
                    row.createCell(1).setCellValue(e.getValue());
                }

                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);
            }

            workbook.write(out);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel", e);
        }
    }

}
