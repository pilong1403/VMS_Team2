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
    public Map<String, Object> getUserRegistrationStats(String rangeType,
                                                        LocalDate from,
                                                        LocalDate to,
                                                        String sort) {
        String rt = (rangeType == null ? "month" : rangeType).toLowerCase();

        // Lấy min/max ngày có user
        LocalDate minDate = reportRepository.findFirstUserCreatedDate();
        LocalDate maxDate = reportRepository.findLastUserCreatedDate();

        Map<String, Object> empty = new HashMap<>();
        if (minDate == null || maxDate == null) {
            empty.put("labels", List.of());
            empty.put("counts", List.of());
            return empty;
        }

        // Chỉ set default nếu người dùng KHÔNG chọn from/to
        if (from == null || to == null) {
            switch (rt) {
                case "year":
                    // hiển thị tất cả các năm có user
                    if (from == null) from = minDate.withDayOfYear(1);
                    if (to   == null) to   = maxDate.withMonth(12).withDayOfMonth(31);
                    break;

                case "month":
                    // mặc định: tháng của NĂM MỚI NHẤT có user
                    int lastYear = maxDate.getYear();
                    if (from == null) from = LocalDate.of(lastYear, 1, 1);
                    if (to   == null) to   = LocalDate.of(lastYear, 12, 31);
                    break;

                case "week":
                    // mặc định lấy khoảng 4 tuần trước ngày đăng ký mới nhất
                    if (from == null) from = maxDate.minusWeeks(4);
                    if (to   == null) to   = maxDate;
                    break;

                default:
                    if (from == null) from = maxDate.minusMonths(1);
                    if (to   == null) to   = maxDate;
            }
        }

        // Lấy dữ liệu thô trong khoảng from/to
        List<Object[]> rawData = reportRepository.countUsersByDateRange(from, to);
        Map<String, Long> grouped = new LinkedHashMap<>();

        if ("week".equals(rt)) {
            WeekFields wf = WeekFields.ISO;
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = "Tuần " + date.get(wf.weekOfMonth()) + " (" + date.getMonthValue() + "/" + date.getYear() + ")";
                grouped.merge(label, count, Long::sum);
            });
        } else if ("month".equals(rt)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = date.getMonthValue() + "/" + date.getYear();
                grouped.merge(label, count, Long::sum);
            });
        } else if ("year".equals(rt)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = String.valueOf(date.getYear());
                grouped.merge(label, count, Long::sum);
            });
        }

        List<String> labels = new ArrayList<>(grouped.keySet());
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
        Map<String, Object> result;
        LocalDate from, to;

        try {
            String rt = rangeType.toLowerCase();

            if ("year".equals(rt)) {
                // label: "2024"  -> drill xuống theo tháng của năm đó
                int year = Integer.parseInt(label.trim());
                from = LocalDate.of(year, 1, 1);
                to   = LocalDate.of(year, 12, 31);

                result = getUserRegistrationStats("month", from, to, "asc");
                result.put("rangeType", "month");
                return result;

            } else if ("month".equals(rt)) {
                // label: "10/2025" -> drill xuống theo tuần của tháng đó
                String[] parts = label.split("/");
                int month = Integer.parseInt(parts[0]);
                int year  = Integer.parseInt(parts[1]);

                from = LocalDate.of(year, month, 1);
                to   = from.plusMonths(1).minusDays(1);

                result = getUserRegistrationStats("week", from, to, "asc");
                result.put("rangeType", "week");
                return result;
            } else if ("week".equals(rt)) {
                // Không drill sâu hơn
                result = new HashMap<>();
                result.put("labels", List.of());
                result.put("counts", List.of());
                result.put("rangeType", "week");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        result = new HashMap<>();
        result.put("labels", List.of());
        result.put("counts", List.of());
        result.put("rangeType", rangeType);
        return result;
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
            }else if ("status".equalsIgnoreCase(type)) {
                Map<String, Long> st = getUserStatusDistribution();
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Trạng thái");
                header.createCell(1).setCellValue("Số lượng");
                int r = 1;
                for (var e : st.entrySet()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(e.getKey());
                    row.createCell(1).setCellValue(e.getValue());
                }
                sheet.autoSizeColumn(0); sheet.autoSizeColumn(1);
            } else if ("opp".equalsIgnoreCase(type)) {
                // --- Thống kê opportunity theo thời gian ---
                Map<String, Object> stats = getOpportunityStats(rangeType, from, to);
                List<String> labels = (List<String>) stats.get("labels");
                List<Long> counts  = (List<Long>) stats.get("counts");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Thời gian");
                header.createCell(1).setCellValue("Số lượng cơ hội");

                for (int i = 0; i < labels.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(labels.get(i));
                    row.createCell(1).setCellValue(counts.get(i));
                }
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);

            } else if ("oppStatus".equalsIgnoreCase(type)) {
                Map<String, Long> st = getOpportunityStatusDistribution();

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Trạng thái");
                header.createCell(1).setCellValue("Số lượng");

                int r = 1;
                for (var e : st.entrySet()) {
                    Row row = sheet.createRow(r++);
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
    @Override
    public Map<String, Long> getUserStatusDistribution() {
        return reportRepository.countUsersByStatus();
    }

    @Override
    public Map<String, Long> getSummaryCounts() {
        Map<String, Long> s = new LinkedHashMap<>();
        s.put("volunteers",     reportRepository.countUsersByRoleName("VOLUNTEER"));
        s.put("organizations",  reportRepository.countOrganizations());
        s.put("opportunities",  reportRepository.countOpportunities());
        s.put("admins",         reportRepository.countUsersByRoleName("ADMIN"));
        return s;
    }

    @Override
    public Map<String, Object> getOpportunityStats(String rangeType,
                                                   LocalDate from,
                                                   LocalDate to) {
        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null)   to   = LocalDate.now();

        List<Object[]> rawData = reportRepository.countOpportunitiesByDateRange(from, to);
        Map<String, Long> grouped = new LinkedHashMap<>();

        String rt = (rangeType == null ? "month" : rangeType).toLowerCase();
        if ("week".equals(rt)) {
            WeekFields wf = WeekFields.ISO;
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = "Tuần " + date.get(wf.weekOfMonth()) +
                        " (" + date.getMonthValue() + "/" + date.getYear() + ")";
                grouped.merge(label, count, Long::sum);
            });
        } else if ("month".equals(rt)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = date.getMonthValue() + "/" + date.getYear();
                grouped.merge(label, count, Long::sum);
            });
        } else if ("year".equals(rt)) {
            rawData.forEach(obj -> {
                LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                long count = (long) obj[1];
                String label = String.valueOf(date.getYear());
                grouped.merge(label, count, Long::sum);
            });
        }

        List<String> labels = new ArrayList<>(grouped.keySet()); // luôn tăng dần
        List<Long> counts = labels.stream().map(grouped::get).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("counts", counts);
        return result;
    }

    @Override
    public Map<String, Long> getOpportunityStatusDistribution() {
        return reportRepository.countOpportunitiesByStatus();
    }


}
