package com.fptuni.vms.controller;

import com.fptuni.vms.service.ReportService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
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

            @RequestParam(name = "oppRangeType", required = false, defaultValue = "month") String oppRangeType,
            @RequestParam(name = "oppFromDate",  required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate oppFromDate,
            @RequestParam(name = "oppToDate",    required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate oppToDate,
            Model model
    ) {
        Map<String, Object> userStats = reportService.getUserRegistrationStats(rangeType, fromDate, toDate, sort);
        Map<String, Long> roleDistribution = reportService.getUserRoleDistribution();
        Map<String, Long> statusDistribution = reportService.getUserStatusDistribution(); // NEW
        Map<String, Long> summary = reportService.getSummaryCounts();

        Map<String, Object> oppStats = reportService.getOpportunityStats(oppRangeType, oppFromDate, oppToDate);
        Map<String, Long>   oppStatusDistribution = reportService.getOpportunityStatusDistribution();

        model.addAttribute("activePage", "reports");
        model.addAttribute("rangeType", rangeType);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("userStats", userStats);
        model.addAttribute("roleDistribution", roleDistribution);
        model.addAttribute("statusDistribution", statusDistribution); // NEW
        model.addAttribute("summary", summary);

        model.addAttribute("oppRangeType",  oppRangeType);
        model.addAttribute("oppFromDate",   oppFromDate);
        model.addAttribute("oppToDate",     oppToDate);
        model.addAttribute("oppStats",      oppStats);
        model.addAttribute("oppStatusDistribution", oppStatusDistribution);

        return "admin/reportManagement";
    }

    @GetMapping("/drilldown")
    @ResponseBody
    public Map<String, Object> drillDown(
            @RequestParam String rangeType,
            @RequestParam String label) {
        return reportService.getDrillDownStats(rangeType, label);
    }

    @GetMapping("/export")
    public void exportReport(
            @RequestParam String type,
            @RequestParam(required = false) String rangeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpServletResponse response) throws IOException {

        String filename = type + "_report_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (ServletOutputStream out = response.getOutputStream()) {
            reportService.exportReportToExcel(type, rangeType, fromDate, toDate, out);
        }
    }

}
