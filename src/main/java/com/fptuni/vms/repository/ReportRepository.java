package com.fptuni.vms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportRepository {
    List<Object[]> countUsersByDateRange(LocalDate from, LocalDate to);
    Map<String, Long> countUsersByRole();
    List<Object[]> countUsersByWeek(LocalDate from, LocalDate to);
    List<Object[]> countUsersByMonth(LocalDate from, LocalDate to);
}
