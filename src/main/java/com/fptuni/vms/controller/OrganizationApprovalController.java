package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.SecurityUtils;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/organization/approvals")
public class OrganizationApprovalController {

    private final OpportunityService opportunityService;
    private final OrganizationService organizationService;

    public OrganizationApprovalController(OpportunityService opportunityService,
            OrganizationService organizationService) {
        this.opportunityService = opportunityService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status, // open|closed|cancelled|all|null
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "num", defaultValue = "10") int size,
            @RequestParam(value = "timeOrder", required = false) String timeOrder, // asc|desc (UI giữ nguyên)
            Model model) {


        model.addAttribute("activePage","approval"); // hiển thị highlight trên sidebar

        // Lấy tổ chức của owner hiện tại
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            model.addAttribute("error", "Không tìm thấy thông tin tổ chức hợp lệ.");
            model.addAttribute("oppList", java.util.List.of());
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", 1);
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 1);
            return "organization/approvals";
        }

        // Nếu user chọn "all" thì để status = null để không lọc theo trạng thái
        String st = (status != null && status.equalsIgnoreCase("all")) ? null : status;

        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
        Page<Opportunity> oppPage = opportunityService.getOrgOpportunities(
                org.getOrgId(),
                null, // categoryId
                keyword, // keyword trong title/subtitle/location
                st, // OPEN|CLOSED|CANCELLED|null
                null, // quick: upcoming|ongoing|past
                "newest", // sortBy
                pageable);

        // Tính dải trang kiểu giao diện điểm danh
        int totalPages = oppPage.getTotalPages() == 0 ? 1 : oppPage.getTotalPages();
        int currentPage = page;
        int window = 2;
        int startPage = Math.max(1, currentPage - window);
        int endPage = Math.min(totalPages, currentPage + window);

        model.addAttribute("oppList", oppPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        // giữ lại filter
        model.addAttribute("num", size);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("timeOrder", timeOrder);

        // dùng để build URL ứng dụng
        model.addAttribute("orgId", org.getOrgId());

        return "organization/approvals";
    }
}
