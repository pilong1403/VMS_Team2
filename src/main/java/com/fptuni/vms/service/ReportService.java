package com.fptuni.vms.service;

import java.time.LocalDate;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getUserRegistrationStats(String rangeType, LocalDate from, LocalDate to, String sort);
    Map<String, Long> getUserRoleDistribution();
    Map<String, Object> getDrillDownStats(String rangeType, String label);
    void exportReportToExcel(String type, String rangeType, LocalDate from, LocalDate to, java.io.OutputStream out);
    Map<String, Long> getUserStatusDistribution();
    Map<String, Long> getSummaryCounts();
    Map<String, Object> getOpportunityStats(String rangeType, LocalDate from, LocalDate to);

    Map<String, Long> getOpportunityStatusDistribution();

}
