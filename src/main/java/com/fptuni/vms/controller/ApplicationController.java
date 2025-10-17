package com.fptuni.vms.controller;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;

    public ApplicationController(ApplicationService applicationService, ApplicationRepository applicationRepository) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
    }

    //  1. Hiển thị form apply (GET)
    @GetMapping("/apply/{opportunityId}")
    public String showApplyForm(@PathVariable Integer opportunityId, Model model) {
        model.addAttribute("opportunityId", opportunityId);
        model.addAttribute("volunteerActive", true);
        return "volunteer/apply-form"; // templates/volunteer/apply-form.html
    }

    //  2. Xử lý khi submit form apply (POST)
    @PostMapping("/apply/{opportunityId}")
    public String submitApplication(
            @PathVariable Integer opportunityId,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam(required = false) MultipartFile cv,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        Integer userId = null;

        try {
            // Gọi service để tạo và lưu đơn vào DB
            Application app = applicationService.applyOpportunity(opportunityId, email, reason);

            // Lấy userId từ volunteer trong Application
            if (app.getVolunteer() != null) {
                userId = app.getVolunteer().getUserId();
            } else {
                userId = applicationService.getVolunteerIdByEmail(email);
            }

            // Thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", "Ứng tuyển thành công!");
            System.out.println(" Lưu đơn ứng tuyển thành công cho email: " + email);

        } catch (RuntimeException e) {
            System.err.println(" Lỗi khi nộp đơn: " + e.getMessage());

            // Nếu lỗi, vẫn thử lấy userId để redirect
            try {
                userId = applicationService.getVolunteerIdByEmail(email);
            } catch (Exception ignored) {
            }

            // Gửi lỗi cho giao diện
            redirectAttributes.addFlashAttribute("globalError", e.getMessage());
        }

        // 🔹 Redirect mặc định sang /applications/mine/{userId}
        if (userId == null || userId <= 0) {
            return "redirect:/applications/mine";
        }

        return "redirect:/applications/mine/" + userId;
    }

    //  3. Hiển thị danh sách đơn của volunteer
    @GetMapping("/mine/{userId}")
    public String viewMyApplications(
            @PathVariable Integer userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model) {

        if (userId == null || userId <= 0) {
            model.addAttribute("applications", java.util.Collections.emptyList());
            model.addAttribute("error", "Không xác định được tình nguyện viên!");
            return "volunteer/application-list";
        }

        var apps = applicationService.getApplicationsByVolunteerId(userId, q, status, sort);

        //  Gửi dữ liệu về Thymeleaf
        model.addAttribute("applications", apps);
        model.addAttribute("paramQ", q);
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramSort", sort);

        return "volunteer/application-list";
    }

    // 👉 Xem chi tiết đơn ứng tuyển
    @GetMapping("/detail/{appId}")
    public String viewApplicationDetail(@PathVariable Integer appId, Model model) {
        Application app = applicationService.getApplicationDetail(appId);
        if (app == null) {
            model.addAttribute("error", "Không tìm thấy đơn ứng tuyển!");
        } else {
            model.addAttribute("application", app);
        }
        return "volunteer/application-detail";
    }

    @PostMapping("/cancel/{appId}")
    public String cancelApplication(@PathVariable Integer appId,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes ra) {
        // Lấy volunteerId trước khi thay đổi (đã join fetch volunteer)
        Integer userId = applicationRepository.findByIdWithVolunteer(appId)
                .map(a -> a.getVolunteer().getUserId())
                .orElse(null);

        try {
            applicationService.cancelApplication(appId, reason);
            ra.addFlashAttribute("successMessage", "Đã huỷ đơn ứng tuyển!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("globalError", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("globalError", "Có lỗi khi huỷ đơn, vui lòng thử lại.");
        }

        return (userId == null) ? "redirect:/applications/mine"
                : "redirect:/applications/mine/" + userId;
    }

}
