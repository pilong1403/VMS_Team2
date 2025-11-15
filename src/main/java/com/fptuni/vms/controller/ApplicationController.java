package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.Feedback;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.OpportunitySectionService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.FeedbackService;
import com.fptuni.vms.service.UserService; // NEW

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class ApplicationController {

    private static final Set<Opportunity.OpportunityStatus> PUBLIC_STATUSES = Set.of(Opportunity.OpportunityStatus.OPEN,
            Opportunity.OpportunityStatus.CLOSED,
            Opportunity.OpportunityStatus.CANCELLED);

    private final ApplicationService applicationService;
    private final OpportunitySectionService sectionService;
    private final OrganizationService organizationService;
    private final OpportunityService opportunityService;
    private final FeedbackService feedbackService;
    private final UserService userService;

    public ApplicationController(ApplicationService applicationService,
            OpportunitySectionService sectionService,
            OrganizationService organizationService,
            OpportunityService opportunityService,
            FeedbackService feedbackService,
            UserService userService) {
        this.applicationService = applicationService;
        this.sectionService = sectionService;
        this.organizationService = organizationService;
        this.opportunityService = opportunityService;
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    // ====== OPPORTUNITY DETAIL ======
    @GetMapping("/opportunities/{id}")
    public String view(@PathVariable Integer id, Model model, HttpSession session) {
        // Lấy opp qua service
        Opportunity opp = opportunityService.findById(id);
        if (opp == null) {
            model.addAttribute("error", "Không tìm thấy cơ hội.");
            model.addAttribute("reviews", List.of());
            model.addAttribute("sections", List.of());
            return "opportunity/opportunity-detail";
        }

        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        model.addAttribute("currentUserId", currentUserId);

        String currentUserRole = (String) session.getAttribute("AUTH_ROLE");
        model.addAttribute("currentUserRole", currentUserRole);

        // Quyền xem: công khai hoặc chủ tổ chức
        boolean isOwner = isOwner(currentUserId, opp); // Chỉ ORG xem được bản opp chưa công bố của chính họ

        // Các trạng thái của opportunity OPEN / CLOSED / CANCELLED =
        // PUBLIC_STATUSES.contains(opp.getStatus());
        // Chỉ 3 trạng thái này được xem công khai trên hệ thống
        // OPEN → đang mở cho TNV xem
        // CLOSED → đã đóng nhưng vẫn hiển thị lịch sử
        // CANCELLED → đã hủy nhưng vẫn công khai cho minh bạch
        boolean isPublic = opp.getStatus() != null && PUBLIC_STATUSES.contains(opp.getStatus());

        if (!isPublic && !isOwner) {
            model.addAttribute("error", "Không tìm thấy cơ hội hoặc bạn không đủ quyền xem.");
            model.addAttribute("reviews", List.of()); // Hệ thống không tìm thấy cơ hội hoặc bạn không đủ quyền xem.
            model.addAttribute("sections", List.of());// List trống để tránh lỗi Thymeleaf
            return "opportunity/opportunity-detail";
        }

        // model render ra thông tin opportunity detail
        model.addAttribute("opp", opp);
        model.addAttribute("org", opp.getOrganization());
        if (opp.getOrganization() != null && opp.getOrganization().getOwner() != null) {
            model.addAttribute("orgOwner", opp.getOrganization().getOwner());
        }

        if (currentUserId != null) {
            // "item" lấy danh sách đơn ứng tuyển của TNV hiện tại
            // tự động điền thông tin vào form apply
            // kiểm tra trùng thời gian apply
            // hiển thị nút “Xem kết quả đơn ứng tuyển”
            // hiển thị cảnh báo đã apply
            // xử lý logic trong modal apply
            model.addAttribute("items", applicationService.listMyApplications(currentUserId));
            User currentUser = userService.getUserById(currentUserId);
            model.addAttribute("currentUser", currentUser);
        } else {
            model.addAttribute("items", List.of());
            model.addAttribute("currentUser", null);
        }

        // Trạng thái & nút hành động
        boolean isCancelled = opp.getStatus() == Opportunity.OpportunityStatus.CANCELLED;

        // nếu startTime <= now -> đã quá hạn -> tắt nút apply
        boolean isExpired = opp.getStartTime() != null && !opp.getStartTime().isAfter(LocalDateTime.now());

        // Kiểm tra đã đủ số lượng volunteer chưa
        long approvedCount = applicationService.countApprovedByOppId(opp.getOppId());
        Integer needVols = opp.getNeededVolunteers();
        boolean isFull = (needVols != null) && (approvedCount >= needVols);

        // Kiểm tra đã apply chưa
        boolean alreadyApplied = currentUserId != null
                && applicationService.existsByOppIdAndVolunteerId(opp.getOppId(), currentUserId);

        // Điều kiện tổng hợp để bấm nút Apply
        boolean canApply = (opp.getStatus() == Opportunity.OpportunityStatus.OPEN)
                && !isExpired
                && !isFull
                && !isCancelled
                && currentUserId != null
                && !alreadyApplied;

        // Set model attributes cho view dùng hiện thị nút đăng ký
        model.addAttribute("isCancelled", isCancelled);
        model.addAttribute("isExpired", isExpired);
        model.addAttribute("isFull", isFull);
        model.addAttribute("canApply", canApply);
        model.addAttribute("alreadyApplied", alreadyApplied);
        model.addAttribute("appliedCount", approvedCount);

        // Badge hiện thị trạng thái
        model.addAttribute("statusDisplayName", toStatusDisplay(opp.getStatus()));
        model.addAttribute("statusBadgeClass", toStatusBadgeClass(opp.getStatus()));

        // Load sections mô tả cơ hội
        List<OpportunitySection> sections = sectionService.findByOpportunity(opp.getOppId());
        model.addAttribute("sections", sections);

        // Load feedback + điều kiện sửa feedback
        boolean isEventEnded = opp.getEndTime() != null && opp.getEndTime().isBefore(LocalDateTime.now());

        if (isEventEnded) {
            // Nếu event đã kết thúc:

            List<Feedback> feedbacks = feedbackService.findByOpportunity(opp.getOppId());
            model.addAttribute("reviews", feedbacks);

            if (currentUserId != null) {
                // Nếu user có feedback của chính họ
                var myFb = feedbackService.findByOpportunityAndVolunteer(opp.getOppId(), currentUserId);
                model.addAttribute("myFeedback", myFb);

                // Kiểm tra được phép tạo/sửa feedback hay không
                boolean canCreateMyFeedback = feedbackService.canVolunteerGiveFeedback(opp.getOppId(), currentUserId);
                model.addAttribute("canCreateMyFeedback", canCreateMyFeedback);

                // Sửa feedback chỉ từ 3 ngày trở lại
                boolean canEditMyFeedback = false;
                if (myFb != null && myFb.getCreatedAt() != null) {
                    canEditMyFeedback = myFb.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3));
                }
                model.addAttribute("canEditMyFeedback", canEditMyFeedback);
            } else {
                model.addAttribute("myFeedback", null);
                model.addAttribute("canCreateMyFeedback", false);
                model.addAttribute("canEditMyFeedback", false);
            }
        } else {
            model.addAttribute("reviews", List.of());
            model.addAttribute("myFeedback", null);
            model.addAttribute("canCreateMyFeedback", false);
            model.addAttribute("canEditMyFeedback", false);
        }

        return "opportunity/opportunity-detail";
    }

    /** Submit đơn đăng ký */
    @PostMapping("/applications/apply")
    public String apply(@RequestParam("oppId") Integer oppId,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address,
            RedirectAttributes ra /** truyền giá trị giữa các controller khi redirect */
    ) {
        try {
            applicationService.apply(oppId, userId, reason, fullName, phone, address);
            ra.addFlashAttribute("success", "Bạn đã gửi đơn đăng ký thành công, vui lòng chờ xét duyệt đơn!");
            return "redirect:/opportunities/" + oppId;
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Có lỗi không mong muốn. Vui lòng thử lại.");
        }
        return "redirect:/opportunities/" + oppId;
    }

    // ====== ORG LIST REDIRECT (KHÔNG CẦN orgId) ======
    @GetMapping("/organization/applications")
    public String redirectMyOrgApplications(@RequestParam Map<String, String> allParams, HttpSession session) {
        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
        }
        Organization myOrg = organizationService.findByOwnerId(currentUserId);
        if (myOrg == null) {
            return "redirect:/?e=ORG_NOT_FOUND_FOR_OWNER";
        }
        String qs = buildQueryString(allParams);
        return "redirect:/organization/" + myOrg.getOrgId() + "/applications" + (qs.isBlank() ? "" : "?" + qs);
    }

    // ====== ORG LIST (FILTER/PAGING) ======
    @GetMapping("/organization/{orgId}/applications")
    public String listApplicationsByOrganization(
            @PathVariable Integer orgId,
            @RequestParam(value = "oppId", required = false) Integer oppId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model, HttpSession session,
            @RequestParam Map<String, String> allParams) {

        model.addAttribute("activePage", "approval");

        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
        }

        Organization myOrg = organizationService.findByOwnerId(currentUserId);
        if (myOrg == null) {
            return "redirect:/?e=ORG_NOT_FOUND_FOR_OWNER";
        }

        // Bảo vệ: nếu URL orgId != org của user => redirect về org của user, giữ query
        if (!myOrg.getOrgId().equals(orgId)) {
            String qs = buildQueryString(allParams);
            return "redirect:/organization/" + myOrg.getOrgId() + "/applications" + (qs.isBlank() ? "" : "?" + qs);
        }

        if (q != null && q.isBlank())
            q = null;
        if (status != null && status.isBlank())
            status = null;
        if (from != null && to != null && from.isAfter(to)) {
            var t = from;
            from = to;
            to = t;
        }

        var result = applicationService.searchOrgApplicationsByOrgId(
                orgId, oppId, q, status, from, to, Math.max(page, 0), Math.max(size, 1));

        var stats = applicationService.computeOrgAppStats(orgId, oppId, q, status, from, to);

        List<Opportunity> myOpps = opportunityService.findByOrganization(orgId);

        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("orgId", orgId);
        model.addAttribute("oppId", oppId);
        model.addAttribute("myOpps", myOpps);
        model.addAttribute("fromStr", from != null ? from.format(fmt) : "");
        model.addAttribute("toStr", to != null ? to.format(fmt) : "");
        model.addAttribute("page", result);
        model.addAttribute("stats", stats);
        model.addAttribute("q", q);
        model.addAttribute("status", status);

        return "organization/application-list";
    }

    // ===== helpers =====
    private boolean isOwner(Integer currentUserId, Opportunity opp) {
        if (currentUserId == null || opp == null || opp.getOrganization() == null
                || opp.getOrganization().getOwner() == null) {
            return false;
        }
        return Objects.equals(opp.getOrganization().getOwner().getUserId(), currentUserId);
    }

    private String toStatusDisplay(Opportunity.OpportunityStatus st) {
        if (st == null)
            return "Không rõ";
        return switch (st) {
            case OPEN -> "Đang mở";
            case CLOSED -> "Đã đóng";
            case CANCELLED -> "Đã hủy";
            default -> "Không công khai";
        };
    }

    private String toStatusBadgeClass(Opportunity.OpportunityStatus st) {
        if (st == null)
            return "badge bg-secondary";
        return switch (st) {
            case OPEN -> "badge bg-success";
            case CLOSED -> "badge bg-secondary";
            case CANCELLED -> "badge bg-danger";
            default -> "badge bg-dark";
        };
    }

    // ====== APPROVE / REJECT ======
    @PostMapping("/organization/{orgId}/applications/{appId}/approve")
    public String approveApplication(@PathVariable Integer orgId,
            @PathVariable Integer appId,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes ra,
            HttpSession session) {
        try {
            Integer processedById = (Integer) session.getAttribute("AUTH_USER_ID");
            if (processedById == null)
                return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
            applicationService.approveApplication(orgId, appId, processedById, note);
            ra.addFlashAttribute("success", "Đã duyệt đơn thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể duyệt đơn. Vui lòng thử lại.");
        }
        String qs = keepListParams(allParams);
        return "redirect:/organization/" + orgId + "/applications" + (qs.isBlank() ? "" : "?" + qs);
    }

    @PostMapping("/organization/{orgId}/applications/{appId}/reject")
    public String rejectApplication(@PathVariable Integer orgId,
            @PathVariable Integer appId,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes ra,
            HttpSession session) {
        try {
            Integer processedById = (Integer) session.getAttribute("AUTH_USER_ID");
            if (processedById == null)
                return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
            applicationService.rejectApplication(orgId, appId, processedById, note);
            ra.addFlashAttribute("success", "Đã từ chối đơn.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể từ chối đơn. Vui lòng thử lại.");
        }
        String qs = keepListParams(allParams);
        return "redirect:/organization/" + orgId + "/applications" + (qs.isBlank() ? "" : "?" + qs);
    }

    private String keepListParams(Map<String, String> params) {
        String[] keys = { "oppId", "q", "status", "from", "to", "page", "size" };
        StringBuilder sb = new StringBuilder();
        try {
            for (String k : keys) {
                String v = params.get(k);
                if (v != null && !v.isBlank()) {
                    if (!sb.isEmpty())
                        sb.append('&');
                    sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v != null && !v.isBlank()) {
                if (!sb.isEmpty())
                    sb.append('&');
                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
