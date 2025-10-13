package com.fptuni.vms.service;

import java.time.LocalDate;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getUserRegistrationStats(String rangeType, LocalDate from, LocalDate to, String sort);
    Map<String, Long> getUserRoleDistribution();
}
