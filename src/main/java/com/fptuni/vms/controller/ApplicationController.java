package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.Feedback;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.OpportunitySectionService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.FeedbackService;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicationController {

    private final ApplicationService service;
    private final ApplicationRepository applicationRepository;
    private final OpportunitySectionService sectionService;
    private final OrganizationService organizationService;
    private final OpportunityService opportunityService;
    private final FeedbackService feedbackService;

    public ApplicationController(ApplicationService service,
            ApplicationRepository applicationRepository,
            OpportunitySectionService sectionService,
            OrganizationService organizationService,
            OpportunityService opportunityService,
            FeedbackService feedbackService) {
        this.service = service;
        this.applicationRepository = applicationRepository;
        this.sectionService = sectionService;
        this.organizationService = organizationService;
        this.opportunityService = opportunityService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/opportunities/{id}")
    public String view(@PathVariable Integer id, Model model, HttpSession session) {
        Opportunity opp = applicationRepository.findOpportunityById(id);
        if (opp == null) {
            model.addAttribute("error", "Không tìm thấy cơ hội.");
            model.addAttribute("reviews", Collections.emptyList());
            model.addAttribute("sections", Collections.emptyList());
            return "opportunity/opportunity-detail";
        }

        model.addAttribute("opp", opp);
        model.addAttribute("org", opp.getOrganization());
        if (opp.getOrganization() != null && opp.getOrganization().getOwner() != null) {
            model.addAttribute("orgOwner", opp.getOrganization().getOwner());
        }

        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        model.addAttribute("currentUserId", currentUserId);

        if (currentUserId != null) {
            model.addAttribute("items", service.listMyApplications(currentUserId));
            User currentUser = applicationRepository.findUserById(currentUserId);
            model.addAttribute("currentUser", currentUser);
        } else {
            model.addAttribute("items", Collections.emptyList());
            model.addAttribute("currentUser", null);
        }

        boolean isExpired = opp.getStartTime() != null
                && !opp.getStartTime().isAfter(LocalDateTime.now());

        long approvedCount = service.findApprovedUsersByOppId(opp.getOppId()).size();
        Integer needVols = opp.getNeededVolunteers();
        boolean isFull = (needVols != null) && (approvedCount >= needVols);

        boolean alreadyApplied = currentUserId != null
                && applicationRepository.existsByOppIdAndVolunteerId(opp.getOppId(), currentUserId);

        boolean canApply = (opp.getStatus() == Opportunity.OpportunityStatus.OPEN)
                && !isExpired
                && !isFull
                && currentUserId != null
                && !alreadyApplied;

        model.addAttribute("isExpired", isExpired);
        model.addAttribute("isFull", isFull);
        model.addAttribute("canApply", canApply);
        model.addAttribute("alreadyApplied", alreadyApplied);
        model.addAttribute("appliedCount", approvedCount);

        // sections
        List<OpportunitySection> sections = sectionService.findByOpportunity(opp.getOppId());
        model.addAttribute("sections", sections);

        // chỉ load feedback khi sự kiện kết thúc
        boolean isEventEnded = opp.getEndTime() != null && opp.getEndTime().isBefore(LocalDateTime.now());
        if (isEventEnded) {
            List<Feedback> feedbacks = feedbackService.findByOpportunity(opp.getOppId());
            model.addAttribute("reviews", feedbacks);

            if (currentUserId != null) {
                var myFb = feedbackService.findByOpportunityAndVolunteer(opp.getOppId(), currentUserId);
                model.addAttribute("myFeedback", myFb);

                boolean canCreateMyFeedback = feedbackService.canVolunteerGiveFeedback(opp.getOppId(), currentUserId);
                model.addAttribute("canCreateMyFeedback", canCreateMyFeedback);

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
            model.addAttribute("reviews", Collections.emptyList());
            model.addAttribute("myFeedback", null);
            model.addAttribute("canCreateMyFeedback", false);
            model.addAttribute("canEditMyFeedback", false);
        }

        return "opportunity/opportunity-detail";
    }

    /** Submit đơn đăng ký -> redirect danh sách đơn của volunteer */
    @PostMapping("/applications/apply")
    public String apply(@RequestParam("oppId") Integer oppId,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address,
            RedirectAttributes ra) {
        try {
            service.apply(oppId, userId, reason, fullName, phone, address);
            ra.addFlashAttribute("success", "Bạn đã gửi đơn đăng ký thành công, vui lòng chờ xét duyệt đơn!");
            return "redirect:/opportunities/" + oppId;
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Có lỗi không mong muốn. Vui lòng thử lại.");
        }
        return "redirect:/opportunities/" + oppId;
    }

    /** Route tiện lợi: không cần orgId trong URL, tự tìm theo user và redirect */
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

    /** Danh sách đơn theo tổ chức (filter, paging) — LỌC THEO oppId nếu có */
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

        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
        }

        Organization myOrg = organizationService.findByOwnerId(currentUserId);
        if (myOrg == null) {
            return "redirect:/?e=ORG_NOT_FOUND_FOR_OWNER";
        }

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

        var result = service.searchOrgApplicationsByOrgId(
                orgId, oppId, q, status, from, to, Math.max(page, 0), Math.max(size, 1));

        var stats = service.computeOrgAppStats(orgId, oppId, q, status, from, to);

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

    /** Duyệt đơn */
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
            service.approveApplication(orgId, appId, processedById, note);
            ra.addFlashAttribute("success", "Đã duyệt đơn thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể duyệt đơn. Vui lòng thử lại.");
        }
        String qs = keepListParams(allParams);
        return "redirect:/organization/" + orgId + "/applications" + (qs.isBlank() ? "" : "?" + qs);
    }

    /** Từ chối đơn */
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
            service.rejectApplication(orgId, appId, processedById, note);
            ra.addFlashAttribute("success", "Đã từ chối đơn.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể từ chối đơn. Vui lòng thử lại.");
        }
        String qs = keepListParams(allParams);
        return "redirect:/organization/" + orgId + "/applications" + (qs.isBlank() ? "" : "?" + qs);
    }
}
