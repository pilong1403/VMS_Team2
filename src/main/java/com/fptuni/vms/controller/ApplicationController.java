package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.OpportunitySectionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicationController {

    private final ApplicationService service;
    private final ApplicationRepository applicationRepository;
    private final OpportunitySectionService sectionService;

    public ApplicationController(ApplicationService service,
            ApplicationRepository applicationRepository, OpportunitySectionService sectionService) {
        this.service = service;
        this.applicationRepository = applicationRepository;
        this.sectionService = sectionService;
    }

    /** Trang chi tiết cơ hội */
    @GetMapping("/opportunities/{id}")
    public String view(@PathVariable Integer id, Model model, HttpSession session) {
        Opportunity opp = applicationRepository.findOpportunityById(id);
        if (opp == null) {
            model.addAttribute("error", "Không tìm thấy cơ hội.");
            return "opportunity/opportunity-detail";
        }
        model.addAttribute("opp", opp);
        model.addAttribute("org", opp.getOrganization());
        // Lấy trưởng ban tổ chức (owner user của org)
        if (opp.getOrganization() != null && opp.getOrganization().getOwner() != null) {
            User orgOwner = opp.getOrganization().getOwner();
            model.addAttribute("orgOwner", orgOwner);
        }

        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId == null)
            return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
        model.addAttribute("items", service.listMyApplications(currentUserId));
        model.addAttribute("currentUserId", currentUserId);

        User currentUser = applicationRepository.findUserById(currentUserId);
        model.addAttribute("currentUser", currentUser);

        // ====== NÚT ĐĂNG KÝ: logic hết hạn/đủ số lượng/đã apply ======
        boolean isExpired = opp.getStartTime() != null
                && !opp.getStartTime().isAfter(LocalDateTime.now()); // now >= startTime -> hết hạn đăng ký

        // Số đơn đã DUYỆT (APPROVED/COMPLETED)
        long approvedCount = service.findApprovedUsersByOppId(opp.getOppId()).size();
        int need = (opp.getNeededVolunteers() != null) ? opp.getNeededVolunteers() : 0;
        boolean isFull = approvedCount >= need;

        boolean alreadyApplied = currentUserId != null
                && applicationRepository.existsByOppIdAndVolunteerId(opp.getOppId(), currentUserId);

        // Chỉ cho phép đăng ký khi: OPEN, chưa đến giờ bắt đầu, chưa đủ người, và chưa
        // apply
        boolean canApply = (opp.getStatus() == Opportunity.OpportunityStatus.OPEN)
                && !isExpired
                && !isFull
                && !alreadyApplied;

        // Đưa biến ra view
        model.addAttribute("isExpired", isExpired);
        model.addAttribute("isFull", isFull);
        model.addAttribute("canApply", canApply);
        model.addAttribute("alreadyApplied", alreadyApplied);
        // Hiển thị số người đã duyệt / cần
        model.addAttribute("appliedCount", approvedCount);

        // NẠP SECTION từ DB
        List<OpportunitySection> sections = sectionService.findByOpportunity(opp.getOppId());

        // Lấy section_order = 1
        OpportunitySection s1 = sections.stream()
                .filter(s -> s.getSectionOrder() != null && s.getSectionOrder() == 1)
                .findFirst().orElse(null);

        // Lấy section_order = 2
        OpportunitySection s2 = sections.stream()
                .filter(s -> s.getSectionOrder() != null && s.getSectionOrder() == 2)
                .findFirst().orElse(null);

        // Đổ ra model cho view dùng
        if (s1 != null) {
            model.addAttribute("introHeading", s1.getHeading());
            model.addAttribute("intro", s1.getContent());
            model.addAttribute("detailImageUrl", s1.getImageUrl());
            model.addAttribute("imageCaption", s1.getCaption());
        }
        if (s2 != null) {
            model.addAttribute("reqHeading", s2.getHeading());
            model.addAttribute("requirement", s2.getContent());
        }

        return "opportunity/opportunity-detail";
    }

    // Submit đơn đăng ký -> redirect danh sách đơn của volunteer
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
            ra.addFlashAttribute("success",
                    "Bạn đã gửi đơn đăng ký thành công, vui lòng chờ xét duyệt đơn!");
            // chuyển tới trang danh sách đơn
            return "redirect:/profile/applications";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Có lỗi không mong muốn. Vui lòng thử lại.");
        }
        // lỗi thì quay lại chi tiết cơ hội
        return "redirect:/opportunities/" + oppId;
    }

    // Danh sách đơn theo tổ chức (với filter, paging) - Phi Long iter 2
    @GetMapping("/organization/{orgId}/applications")
    public String listApplicationsByOrganization(
            @PathVariable Integer orgId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model, HttpSession session) {

        // chuẩn hoá input
        if (q != null && q.isBlank())
            q = null;
        if (status != null && status.isBlank())
            status = null;
        if (from != null && to != null && from.isAfter(to)) {
            var t = from;
            from = to;
            to = t;
        }

        var result = service.searchOrgApplicationsByOrgId(orgId, q, status, from, to, Math.max(page, 0),
                Math.max(size, 1));
        var stats = service.computeOrgAppStats(orgId);

        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Integer currentUserId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (currentUserId == null)
            return "redirect:/login?e=USERNAME_PASSWORD_REQUIRED";
        model.addAttribute("currentUserId", currentUserId);

        model.addAttribute("fromStr", from != null ? from.format(fmt) : "");
        model.addAttribute("toStr", to != null ? to.format(fmt) : "");

        model.addAttribute("page", result);
        model.addAttribute("stats", stats);
        model.addAttribute("orgId", orgId);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "organization/application-list";
    }

    // ===== helpers để giữ query khi quay lại list =====
    private String keepListParams(Map<String, String> params) {
        // chỉ pick các khóa được sử dụng
        String[] keys = { "q", "status", "from", "to", "page", "size" };
        StringBuilder sb = new StringBuilder();
        try {
            for (String k : keys) {
                String v = params.get(k);
                if (v != null && !v.isBlank()) {
                    if (!sb.isEmpty())
                        sb.append('&');
                    sb.append(java.net.URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8))
                            .append('=')
                            .append(java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    // Duyệt đơn - Phi Long iter2
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

    // Từ chối đơn - Phi Long iter2
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
