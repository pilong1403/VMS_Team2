package com.fptuni.vms.controller;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.service.CategoryService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class OpportunityForOrgController {
    private final OpportunityService opportunityService;
    private final CategoryService categoryService;
    private final OrganizationService organizationService; // hoặc OrganizationRepository

    public OpportunityForOrgController(OpportunityService opportunityService,
            CategoryService categoryService,
            OrganizationService organizationService) {
        this.opportunityService = opportunityService;
        this.categoryService = categoryService;
        this.organizationService = organizationService;
    }

    // === Trang tổ chức – grid + filters === PhiLong iter 3
    @GetMapping("/organizations/{orgId}")
    public String viewOrganization(
            @PathVariable @Min(1) int orgId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q, // keyword
            @RequestParam(required = false) String status, // OPEN|CLOSED|CANCELLED
            @RequestParam(required = false) String quick, // upcoming|ongoing|past
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {
        Organization org = organizationService.getOrganizationById(orgId);
        if (org == null) {
            model.addAttribute("error", "Không tìm thấy tổ chức");
            return "error/404"; // tuỳ trang lỗi của bạn
        }

        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Opportunity> oppPage = opportunityService.getOrgOpportunities(
                orgId, categoryId, q, status, quick, sort, pr);

        // build Map<oppId, approvedCount> cho progress bar
        Map<Integer, Long> appliedCounts = new HashMap<>();
        for (Opportunity o : oppPage.getContent()) {
            appliedCounts.put(o.getOppId(), opportunityService.countApproved(o.getOppId()));
        }

        List<Category> categories = categoryService.listAll();

        model.addAttribute("org", org);
        model.addAttribute("cards", oppPage); // Page<Opportunity>
        model.addAttribute("appliedCounts", appliedCounts);
        model.addAttribute("categories", categories);
        model.addAttribute("total", oppPage.getTotalElements());

        // giữ lại filter
        model.addAttribute("filter_categoryId", categoryId);
        model.addAttribute("filter_q", q);
        model.addAttribute("filter_status", status);
        model.addAttribute("filter_quick", quick);
        model.addAttribute("filter_sort", sort);

        return "organization/view"; // => templates/organization/view.html
    }// === Trang tổ chức – grid + filters === PhiLong iter 3
}