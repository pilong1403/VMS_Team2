package com.fptuni.vms.controller;

import com.fptuni.vms.model.FAQ;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.AttendanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/organization/attendance")
    public String viewAndFilterAttendance(Model model,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(name = "num", required = false) Integer size,
                                          @RequestParam(required = false) String timeOrder,
                                          @RequestParam(required = false) String status,
                                          @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                          @RequestParam(defaultValue = "1") int page) {

        int recordsPerPage = (size != null && size > 0) ? size : 5;

        Organization organization = attendanceService.findOrganizationByOwnerId(loggedInUser.getUserId());
        Integer orgId = organization.getOrgId();

        List<Opportunity> oppList = attendanceService.filterOpportunities(orgId, status, keyword, timeOrder, page, recordsPerPage);

        long totalOpp = attendanceService.countFilteredOpportunities(orgId, status, keyword);

        int totalPages = (int) Math.ceil((double) totalOpp / recordsPerPage);
        if (totalPages == 0) {
            totalPages = 1;
        }

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);
        if (endPage < startPage) {
            startPage = 1;
            endPage = 1;
        }

        model.addAttribute("oppList", oppList);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        model.addAttribute("status", status);
        model.addAttribute("num", size);
        model.addAttribute("timeOrder", timeOrder);
        model.addAttribute("keyword", keyword);

        model.addAttribute("activePage", "attendance");

        if (oppList == null || oppList.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy sự kiện nào phù hợp!");
        }

        return "attendance/Attendance";
    }

}
