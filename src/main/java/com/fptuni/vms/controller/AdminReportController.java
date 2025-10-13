package com.fptuni.vms.controller;

import com.fptuni.vms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public String reportPage(
            @RequestParam(defaultValue = "month") String rangeType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "desc") String sort,
            Model model
    ) {
        Map<String, Object> userStats = reportService.getUserRegistrationStats(rangeType, fromDate, toDate, sort);
        Map<String, Long> roleDistribution = reportService.getUserRoleDistribution();

        model.addAttribute("rangeType", rangeType);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("sort", sort);
        model.addAttribute("userStats", userStats);
        model.addAttribute("roleDistribution", roleDistribution);
        return "admin/reportManagement";
    }
}
