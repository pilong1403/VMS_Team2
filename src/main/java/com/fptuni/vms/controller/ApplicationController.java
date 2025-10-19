package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // CHÚ Ý: đúng import
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class ApplicationController {

    private final ApplicationService service;
    private final ApplicationRepository applicationRepository;

    public ApplicationController(ApplicationService service,
            ApplicationRepository applicationRepository) {
        this.service = service;
        this.applicationRepository = applicationRepository;
    }

    /** VIEW: GET /opportunities/{id} — hiển thị trang chi tiết cơ hội */
    /** VIEW: GET /opportunities/{id} — hiển thị trang chi tiết cơ hội */
    @GetMapping("/opportunities/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Opportunity opp = applicationRepository.findOpportunityById(id);
        if (opp == null) {
            model.addAttribute("error", "Không tìm thấy cơ hội.");
            return "opportunity/opportunity-detail";
        }

        model.addAttribute("opp", opp);

        Integer currentUserId = 14; // tạm hardcode khi chưa login
        model.addAttribute("currentUserId", currentUserId);

        // ➕ NEW: đếm thật số người đã apply
        long appliedCount = applicationRepository.countByOppId(opp.getOppId());
        model.addAttribute("appliedCount", appliedCount);

        // Hiển thị/ẩn nút Apply
        boolean canApply = true;
        if (opp.getEndTime() != null && !opp.getEndTime().isAfter(LocalDateTime.now()))
            canApply = false;
        if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
            canApply = false;
        if (currentUserId != null &&
                applicationRepository.existsByOppIdAndVolunteerId(opp.getOppId(), currentUserId))
            canApply = false;
        // (tuỳ chọn) khóa khi đã đủ số lượng
        if (opp.getNeededVolunteers() != null && appliedCount >= opp.getNeededVolunteers())
            canApply = false;

        model.addAttribute("canApply", canApply);

        return "opportunity/opportunity-detail";
    }

    /** APPLY: POST /applications/apply — submit form từ Thymeleaf */
    @PostMapping("/applications/apply")
    public String apply(@RequestParam("oppId") Integer oppId,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes ra) {
        try {
            service.apply(oppId, userId, reason);
            ra.addFlashAttribute("success", "Đã gửi đơn đăng ký thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Có lỗi không mong muốn. Vui lòng thử lại.");
        }
        return "redirect:/opportunities/" + oppId;
    }
}
