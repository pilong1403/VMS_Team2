// src/main/java/com/fptuni/vms/controller/OpportunityForOrgController.java
package com.fptuni.vms.controller;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.CategoryService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OpportunityForOrgController {

    private final OpportunityService opportunityService;
    private final OrganizationService organizationService;
    private final CategoryService categoryService;
    private final UserService userService;

    public OpportunityForOrgController(OpportunityService opportunityService,
            OrganizationService organizationService,
            CategoryService categoryService,
            UserService userService) {
        this.opportunityService = opportunityService;
        this.organizationService = organizationService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping("/organizations/{orgId}")
    public String viewOrgOpportunities(@PathVariable("orgId") @Min(1) int orgId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "3") int size,
            @RequestParam(name = "search", required = false) String searchTerm,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sortBy,
            Model model,
            HttpSession session) {

        if (page < 0)
            page = 0;
        if (size <= 0)
            size = 3;

        String repoSort = "newest";
        if (sortBy != null && !sortBy.isBlank()) {
            repoSort = "start_asc".equalsIgnoreCase(sortBy) ? "deadline" : "newest";
        }

        Pageable pageable = PageRequest.of(page, size);

        // 1) Tổ chức
        Organization org = organizationService.getOrganizationById(orgId);
        if (org == null) {
            model.addAttribute("error", "Tổ chức không tồn tại");
            return "error/403";
        }

        // 2) Page cơ hội theo tổ chức + filter
        Page<Opportunity> pageData = opportunityService.getOrgOpportunities(
                orgId,
                categoryId,
                (searchTerm != null && !searchTerm.isBlank()) ? searchTerm.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null,
                null,
                repoSort,
                pageable);

        List<Opportunity> content = pageData.getContent();

        // 3) Tính phát sinh
        Map<Integer, Integer> appliedMap = new HashMap<>();
        Map<Integer, Integer> progressMap = new HashMap<>();
        Map<Integer, String> categoryNameMap = new HashMap<>();

        long openCountOnPage = 0L;
        long volunteerJoinedOnPage = 0L;

        for (Opportunity o : content) {
            long approved = opportunityService.countApproved(o.getOppId());
            int applied = (int) Math.max(0, approved);
            appliedMap.put(o.getOppId(), applied);
            volunteerJoinedOnPage += applied;

            int needed = (o.getNeededVolunteers() != null && o.getNeededVolunteers() > 0)
                    ? o.getNeededVolunteers()
                    : 0;
            int progress = (needed == 0) ? 0 : (int) Math.min(100, Math.round(applied * 100.0 / needed));
            progressMap.put(o.getOppId(), progress);

            categoryNameMap.put(o.getOppId(), (o.getCategory() != null ? o.getCategory().getCategoryName() : null));

            if (o.getStatus() == Opportunity.OpportunityStatus.OPEN)
                openCountOnPage++;
        }

        // 4) Categories
        List<Category> categories = categoryService.listAll();

        // 5) Liên hệ tổ chức
        String orgEmail = null, orgPhone = null;
        boolean orgVerified = (org.getRegStatus() == Organization.RegStatus.APPROVED);
        if (org.getOwner() != null) {
            try {
                orgEmail = org.getOwner().getEmail();
            } catch (Exception ignored) {
            }
            try {
                orgPhone = org.getOwner().getPhone();
            } catch (Exception ignored) {
            }
        }

        // 6) LẤY NGƯỜI DÙNG ĐĂNG NHẬP để PREFILL
        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId != null) {
            model.addAttribute("AUTH_USER_ID", currentUserId); // <-- QUAN TRỌNG: cho JS guard login
            User currentUser = userService.getUserById(currentUserId);
            model.addAttribute("currentUser", currentUser); // <-- dùng để prefill modal (data-*)
        } else {
            model.addAttribute("AUTH_USER_ID", null);
        }

        // 7) Push model
        model.addAttribute("org", org);
        model.addAttribute("orgVerified", orgVerified);
        model.addAttribute("orgEmail", orgEmail);
        model.addAttribute("orgPhone", orgPhone);

        model.addAttribute("categories", categories);
        model.addAttribute("opportunities", content);

        model.addAttribute("appliedMap", appliedMap);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("categoryNameMap", categoryNameMap);

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalElements", pageData.getTotalElements());
        model.addAttribute("hasPrevious", pageData.hasPrevious());
        model.addAttribute("hasNext", pageData.hasNext());

        model.addAttribute("searchTerm", (searchTerm != null && !searchTerm.isBlank()) ? searchTerm : null);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", (status != null && !status.isBlank()) ? status.toUpperCase() : null);
        model.addAttribute("selectedSort", sortBy);

        model.addAttribute("openCount", openCountOnPage);
        model.addAttribute("volunteerJoined", volunteerJoinedOnPage);

        return "organization/org-opportunities";
    }
}
