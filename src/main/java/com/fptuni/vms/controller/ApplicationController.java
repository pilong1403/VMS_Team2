package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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

    /** Trang chi tiết cơ hội */
    @GetMapping("/opportunities/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Opportunity opp = applicationRepository.findOpportunityById(id);
        if (opp == null) {
            model.addAttribute("error", "Không tìm thấy cơ hội.");
            return "opportunity/opportunity-detail";
        }
        model.addAttribute("opp", opp);

        // tạm hardcode user đang test (thống nhất = 10)
        Integer currentUserId = 11;
        model.addAttribute("currentUserId", currentUserId);

        // Prefill profile cho popup
        User currentUser = applicationRepository.findUserById(currentUserId);
        model.addAttribute("currentUser", currentUser);

        // Điều kiện hiển thị nút "Đăng ký tham gia"
        boolean canApply = true;
        if (opp.getEndTime() != null && !opp.getEndTime().isAfter(LocalDateTime.now()))
            canApply = false;
        if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
            canApply = false;
        if (currentUserId != null &&
                applicationRepository.existsByOppIdAndVolunteerId(opp.getOppId(), currentUserId)) {
            canApply = false;
        }
        model.addAttribute("canApply", canApply);

        // Số người đã apply thực tế
        model.addAttribute("appliedCount", applicationRepository.countByOppId(opp.getOppId()));

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
            return "redirect:/volunteer/applications";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Có lỗi không mong muốn. Vui lòng thử lại.");
        }
        // lỗi thì quay lại chi tiết cơ hội
        return "redirect:/opportunities/" + oppId;
    }

    // Danh sách đơn của volunteer
    @GetMapping("/volunteer/applications")
    public String myApplications(Model model) {
        Integer currentUserId = 11; // TODO: thay bằng ID từ session/auth
        model.addAttribute("items", service.listMyApplications(currentUserId));
        return "volunteer/my-applications";
    }

}
