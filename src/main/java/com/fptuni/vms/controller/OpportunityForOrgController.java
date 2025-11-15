// src/main/java/com/fptuni/vms/controller/OpportunityForOrgController.java
package com.fptuni.vms.controller;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class OpportunityForOrgController {

    private final OpportunityService opportunityService;
    private final OrganizationService organizationService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final ApplicationService applicationService; // ⭐ NEW for logic Apply

    public OpportunityForOrgController(
            OpportunityService opportunityService,
            OrganizationService organizationService,
            CategoryService categoryService,
            UserService userService,
            ApplicationService applicationService // ⭐ NEW
    ) {
        this.opportunityService = opportunityService;
        this.organizationService = organizationService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.applicationService = applicationService; // ⭐ NEW
    }

    @GetMapping("/organizations/{orgId}")
    public String viewOrgOpportunities(
            @PathVariable("orgId") @Min(1) int orgId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "3") int size,
            @RequestParam(name = "search", required = false) String searchTerm,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sortBy,
            Model model,
            HttpSession session) {

        // ---- Paging ----
        if (page < 0)
            page = 0;
        if (size <= 0)
            size = 3;
        Pageable pageable = PageRequest.of(page, size);

        String repoSort = "newest";
        if (sortBy != null && !sortBy.isBlank()) {
            repoSort = "start_asc".equalsIgnoreCase(sortBy) ? "deadline" : "newest";
        }

        // ---- Organization ----
        Organization org = organizationService.getOrganizationById(orgId);
        if (org == null) {
            model.addAttribute("error", "Tổ chức không tồn tại");
            return "error/403";
        }

        // ---- Fetch opportunities ----
        Page<Opportunity> pageData = opportunityService.getOrgOpportunities(
                orgId,
                categoryId,
                (searchTerm != null && !searchTerm.isBlank()) ? searchTerm.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null,
                null,
                repoSort,
                pageable);

        List<Opportunity> content = pageData.getContent();

        // ---- Applied / Progress Map ----
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

            categoryNameMap.put(o.getOppId(),
                    (o.getCategory() != null ? o.getCategory().getCategoryName() : null));

            if (o.getStatus() == Opportunity.OpportunityStatus.OPEN)
                openCountOnPage++;
        }

        // ---- Categories ----
        List<Category> categories = categoryService.listAll();

        // ---- Current User ----
        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId != null) {
            User u = userService.getUserById(currentUserId);
            model.addAttribute("currentUser", u);
            model.addAttribute("AUTH_USER_ID", currentUserId);

            // ⭐ thêm dòng này
            model.addAttribute("currentUserRole", u.getRole().getRoleName());
        } else {
            model.addAttribute("currentUser", null);
            model.addAttribute("AUTH_USER_ID", null);

            // ⭐ nếu chưa login → role null
            model.addAttribute("currentUserRole", null);
        }

        // NEW — Compute Apply Button Logic (giống /opportunities)
        Map<Integer, Map<String, Object>> btnStates = computeButtonStates(content, currentUserId);
        model.addAttribute("btnStates", btnStates);

        // ---- Org contact ----
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

        // ---- Push to model ----
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

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sortBy);

        model.addAttribute("openCount", openCountOnPage);
        model.addAttribute("volunteerJoined", volunteerJoinedOnPage);

        return "organization/org-opportunities";
    }

    // ⭐ NEW — copy logic từ HomeController
    private Map<Integer, Map<String, Object>> computeButtonStates(List<Opportunity> list, Integer userId) {
        if (list == null || list.isEmpty())
            return Map.of();

        Map<Integer, Map<String, Object>> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Count approved & applied
        Map<Integer, Long> approvedByOpp = new HashMap<>();
        Map<Integer, Boolean> appliedByOpp = new HashMap<>();

        for (Opportunity o : list) {
            int oppId = o.getOppId();
            approvedByOpp.put(oppId, opportunityService.countApproved(o.getOppId()));

            if (userId != null) {
                boolean alreadyApplied = applicationService.existsByOppIdAndVolunteerId(oppId, userId);
                appliedByOpp.put(oppId, alreadyApplied);
            }
        }

        for (Opportunity o : list) {
            int oppId = o.getOppId();

            Opportunity.OpportunityStatus st = o.getStatus();
            LocalDateTime start = o.getStartTime();
            Integer needVol = o.getNeededVolunteers();

            boolean isCancelled = (st == Opportunity.OpportunityStatus.CANCELLED);
            boolean isExpired = (start != null && !start.isAfter(now));
            long approved = approvedByOpp.getOrDefault(oppId, 0L);
            boolean isFull = (needVol != null && approved >= needVol);
            boolean alreadyApplied = appliedByOpp.getOrDefault(oppId, false);

            boolean canApply = (st == Opportunity.OpportunityStatus.OPEN)
                    && !isCancelled
                    && !isExpired
                    && !isFull
                    && userId != null
                    && !alreadyApplied;

            Map<String, Object> one = new HashMap<>();
            one.put("isCancelled", isCancelled);
            one.put("isExpired", isExpired);
            one.put("isFull", isFull);
            one.put("alreadyApplied", alreadyApplied);
            one.put("canApply", canApply);
            one.put("appliedCount", approved);
            one.put("neededVolunteers", needVol == null ? 0 : needVol);

            result.put(oppId, one);
        }

        return result;
    }
}
