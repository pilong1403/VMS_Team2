package com.fptuni.vms.service.impl;

import com.fptuni.vms.repository.ReportRepository;
import com.fptuni.vms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
