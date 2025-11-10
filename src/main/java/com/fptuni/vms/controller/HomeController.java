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
import org.springframework.data.domain.PageImpl;
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

    private static final Set<String> ALLOWED_STATUS = Set.of("OPEN", "CLOSED", "CANCELLED");
    private static final Set<String> ALLOWED_TIME = Set.of("today", "week", "month");

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
            if (currentUserId != null) {
                model.addAttribute("currentUserId", currentUserId);
                model.addAttribute("currentUser", userService.getUserById(currentUserId));
            }
            List<OpportunityCardDto> latestOpportunities = opportunityService.getTop3LatestOpportunities();
            // Ẩn DRAFT ở khu “Mới đăng”
            latestOpportunities = latestOpportunities.stream()
                    .filter(this::isAllowedStatus)
                    .toList();

            model.addAttribute("latestOpportunities", latestOpportunities);
            Map<Integer, Map<String, Object>> latestBtnStates = computeButtonStates(latestOpportunities, currentUserId);
            model.addAttribute("latestBtnStates", latestBtnStates);

            // Thêm thống kê
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

            // Thêm thống kê cho trang about
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
            if (currentUserId != null) {
                model.addAttribute("currentUserId", currentUserId);
                model.addAttribute("currentUser", userService.getUserById(currentUserId));
            }

            StringBuilder warning = new StringBuilder();

            // ---- Sanitize status/time (coi chuỗi rỗng là không lọc, KHÔNG cảnh báo) ----
            String safeStatus = sanitizeStatus(status);
            if (status != null && !status.isBlank() && safeStatus == null) {
                appendWarn(warning, "Bộ lọc trạng thái không hợp lệ, chỉ cho phép: OPEN, CLOSED, CANCELLED.");
            }

            String safeTime = sanitizeTime(time);
            if (time != null && !time.isBlank() && safeTime == null) {
                appendWarn(warning, "Bộ lọc thời gian không hợp lệ, chỉ cho phép: today, week, month.");
            }

            // ---- Parse page/size ----
            Integer pageNum = parseIntOrNull(page);
            Integer sizeNum = parseIntOrNull(size);
            if (page != null && pageNum == null) {
                appendWarn(warning, "Tham số 'page' không hợp lệ, đã đặt về 0.");
            }
            if (size != null && sizeNum == null) {
                appendWarn(warning, "Tham số 'size' không hợp lệ, đã đặt về 6.");
            }
            int p = (pageNum != null ? pageNum : 0);
            int s = (sizeNum != null ? sizeNum : 6);
            p = Math.max(p, 0);
            s = Math.min(Math.max(s, 1), 50);

            // ---- Lấy danh mục và tập ID hợp lệ ----
            List<Category> categories = opportunityService.getCategoriesWithOpportunities();
            Set<Integer> allowedCategoryIds = categories.stream()
                    .map(Category::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // ---- Hợp nhất categoryRaw từ categoryId/category ----
            String categoryRaw = (categoryId != null && !categoryId.isBlank()) ? categoryId : category;

            Integer safeCategoryId = null;
            if (categoryRaw != null && !categoryRaw.isBlank()) {
                Integer cat = parseIntOrNull(categoryRaw);
                if (cat == null) {
                    appendWarn(warning, "Tham số 'category' không hợp lệ, đã bỏ lọc.");
                } else if (!allowedCategoryIds.contains(cat)) {
                    appendWarn(warning, "Danh mục không tồn tại, đã bỏ lọc.");
                } else {
                    safeCategoryId = cat;
                }
            }

            Pageable pageable = PageRequest.of(p, s);
            Page<OpportunityCardDto> opportunityPage;

            // Nếu status người dùng nhập không hợp lệ → trả về rỗng + cảnh báo
            if (status != null && !status.isBlank() && safeStatus == null) {
                opportunityPage = new PageImpl<>(List.of(), pageable, 0);
            } else {
                if (safeCategoryId != null || location != null || safeStatus != null || search != null
                        || safeTime != null) {
                    opportunityPage = opportunityService.getOpportunityCardsWithFilters(
                            safeCategoryId, location, safeStatus, search, safeTime, "newest", pageable);
                } else {
                    opportunityPage = opportunityService.getOpportunityCards(pageable);
                }

                // Lọc an toàn để không hiển thị trạng thái cấm (nếu service chưa chặn)
                List<OpportunityCardDto> filtered = opportunityPage.getContent().stream()
                        .filter(this::isAllowedStatus)
                        .collect(Collectors.toList());

                opportunityPage = new PageImpl<>(filtered, pageable,
                        (safeStatus == null) ? opportunityPage.getTotalElements() : filtered.size());
            }

            Map<Integer, Map<String, Object>> btnStates = computeButtonStates(opportunityPage.getContent(),
                    currentUserId);

            model.addAttribute("opportunities", opportunityPage.getContent());
            model.addAttribute("currentPage", p);
            model.addAttribute("totalPages", Math.max(opportunityPage.getTotalPages(), 1));
            model.addAttribute("totalElements", opportunityPage.getTotalElements());
            model.addAttribute("hasNext", opportunityPage.hasNext());
            model.addAttribute("hasPrevious", opportunityPage.hasPrevious());

            model.addAttribute("categories", categories);
            model.addAttribute("selectedCategoryId", safeCategoryId);
            model.addAttribute("selectedLocation", location);
            model.addAttribute("selectedStatus", safeStatus);
            model.addAttribute("searchTerm", search);
            model.addAttribute("selectedTime", safeTime);

            model.addAttribute("btnStates", btnStates);

            if (warning.length() > 0) {
                model.addAttribute("filterWarning", warning.toString());
            }

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

    // ===== Helpers =====

    private static void appendWarn(StringBuilder sb, String msg) {
        if (sb.length() > 0)
            sb.append(' ');
        sb.append(msg);
    }

    private Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String sanitizeStatus(String status) {
        if (status == null || status.isBlank())
            return null; // coi rỗng là không lọc
        String s = status.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_STATUS.contains(s) ? s : null;
    }

    private String sanitizeTime(String time) {
        if (time == null || time.isBlank())
            return null; // coi rỗng là không lọc
        String s = time.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_TIME.contains(s) ? s : null;
    }

    private boolean isAllowedStatus(OpportunityCardDto c) {
        if (c == null || c.getStatus() == null)
            return false;
        Opportunity.OpportunityStatus st = c.getStatus();
        return st == Opportunity.OpportunityStatus.OPEN
                || st == Opportunity.OpportunityStatus.CLOSED
                || st == Opportunity.OpportunityStatus.CANCELLED;
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
