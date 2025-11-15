package com.fptuni.vms.controller;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.HomeStatsService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final OpportunityService opportunityService;
    private final UserService userService;
    private final ApplicationService applicationService;
    private final HomeStatsService homeStatsService;

    public HomeController(OpportunityService opportunityService,
            UserService userService,
            ApplicationService applicationService,
            HomeStatsService homeStatsService) {
        this.opportunityService = opportunityService;
        this.userService = userService;
        this.applicationService = applicationService;
        this.homeStatsService = homeStatsService;
    }

    @GetMapping("/home")
    public String volunteerHome(Model model, HttpSession session) {
        try {
            Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
            String currentRole = (String) session.getAttribute("AUTH_ROLE");

            if (currentUserId != null) {
                model.addAttribute("currentUserId", currentUserId);
                model.addAttribute("currentUser", userService.getUserById(currentUserId));
                model.addAttribute("currentUserRole", currentRole);
            } else {
                model.addAttribute("currentUserRole", null);
            }
            List<OpportunityCardDto> latestOpportunities = opportunityService.getTop3LatestOpportunities();
            model.addAttribute("latestOpportunities", latestOpportunities);
            Map<Integer, Map<String, Object>> latestBtnStates = computeButtonStates(latestOpportunities, currentUserId);
            model.addAttribute("latestBtnStates", latestBtnStates);

            Map<String, Object> stats = homeStatsService.getHomeStats();
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            model.addAttribute("latestOpportunities", List.of());
            model.addAttribute("latestBtnStates", Map.of());
            model.addAttribute("stats", Map.of());
        }
        return "home/home";
    }

    @GetMapping("/")
    public String publicHome(Model model, HttpSession session) {
        return volunteerHome(model, session);
    }

    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        try {
            Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
            if (currentUserId != null) {
                model.addAttribute("currentUserId", currentUserId);
                model.addAttribute("currentUser", userService.getUserById(currentUserId));
            }

            Map<String, Object> stats = homeStatsService.getHomeStats();
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            model.addAttribute("stats", Map.of());
        }
        return "public/about";
    }

    @GetMapping("/home/opportunities")
    public String orgOwnerLanding(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false, name = "categoryId") String categoryId,
            @RequestParam(required = false, name = "category") String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String time,
            Model model, HttpSession session) {
        return opportunities(page, size, categoryId, category, location, status, search, time, model, session);
    }

    @GetMapping("/opportunities")
    public String opportunities(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false, name = "categoryId") String categoryId,
            @RequestParam(required = false, name = "category") String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String time,
            Model model, HttpSession session) {

        try {
            Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
            String currentRole = (String) session.getAttribute("AUTH_ROLE");

            if (currentUserId != null) {
                model.addAttribute("currentUserId", currentUserId);
                model.addAttribute("currentUser", userService.getUserById(currentUserId));
                model.addAttribute("currentUserRole", currentRole);
            } else {
                model.addAttribute("currentUserRole", null);
            }

            int p = parseIntOrNull(page, 0);
            int s = parseIntOrNull(size, 6);
            Integer catId = parseIntOrNull(categoryId != null ? categoryId : category, null);

            List<Category> categories = opportunityService.getCategoriesWithOpportunities();
            Pageable pageable = PageRequest.of(p, s);

            Page<OpportunityCardDto> opportunityPage = opportunityService.getOpportunityCardsWithFilters(
                    catId, location, status, search, time, "newest", pageable);

            List<OpportunityCardDto> opportunities = opportunityPage.getContent();
            Map<Integer, Map<String, Object>> btnStates = computeButtonStates(opportunities, currentUserId);

            model.addAttribute("opportunities", opportunities);
            model.addAttribute("currentPage", p);
            model.addAttribute("totalPages", Math.max(opportunityPage.getTotalPages(), 1));
            model.addAttribute("totalElements", opportunityPage.getTotalElements());
            model.addAttribute("hasNext", opportunityPage.hasNext());
            model.addAttribute("hasPrevious", opportunityPage.hasPrevious());
            model.addAttribute("categories", categories);
            model.addAttribute("selectedCategoryId", catId);
            model.addAttribute("selectedLocation", location);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("selectedTime", time);
            model.addAttribute("btnStates", btnStates);

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("opportunities", List.of());
            model.addAttribute("categories", List.of());
            model.addAttribute("totalElements", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("btnStates", Map.of());
        }

        return "home/opportunities";
    }

    private Integer parseIntOrNull(String raw, Integer defaultValue) {
        if (raw == null || raw.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Map<Integer, Map<String, Object>> computeButtonStates(List<OpportunityCardDto> cards,
            Integer currentUserId) {
        if (cards == null || cards.isEmpty())
            return Map.of();

        List<Integer> oppIds = cards.stream()
                .map(OpportunityCardDto::getOppId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Integer, Long> approvedCountByOpp = new HashMap<>();
        for (Integer oppId : oppIds) {
            long approvedCount = applicationService.countApprovedByOppId(oppId);
            approvedCountByOpp.put(oppId, approvedCount);
        }

        Map<Integer, Boolean> alreadyAppliedByOpp = new HashMap<>();
        if (currentUserId != null) {
            for (Integer oppId : oppIds) {
                boolean alreadyApplied = applicationService.existsByOppIdAndVolunteerId(oppId, currentUserId);
                alreadyAppliedByOpp.put(oppId, alreadyApplied);
            }
        }

        Map<Integer, Map<String, Object>> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (var c : cards) {
            Integer oppId = c.getOppId();
            if (oppId == null)
                continue;

            Opportunity.OpportunityStatus st = c.getStatus();
            LocalDateTime start = c.getStartTime();
            Integer needVols = c.getNeededVolunteers();

            boolean isCancelled = (st == Opportunity.OpportunityStatus.CANCELLED);
            boolean isExpired = (start != null) && !start.isAfter(now);
            long approvedCount = approvedCountByOpp.getOrDefault(oppId, 0L);
            boolean isFull = (needVols != null) && (approvedCount >= needVols);
            boolean alreadyApplied = alreadyAppliedByOpp.getOrDefault(oppId, false);

            boolean canApply = (st == Opportunity.OpportunityStatus.OPEN)
                    && !isExpired
                    && !isFull
                    && (currentUserId != null)
                    && !alreadyApplied;

            Map<String, Object> one = new HashMap<>();
            one.put("isCancelled", isCancelled);
            one.put("isExpired", isExpired);
            one.put("isFull", isFull);
            one.put("alreadyApplied", alreadyApplied);
            one.put("canApply", canApply);
            one.put("appliedCount", approvedCount);
            one.put("neededVolunteers", needVols == null ? 0 : needVols);
            result.put(oppId, one);
        }
        return result;
    }
}
