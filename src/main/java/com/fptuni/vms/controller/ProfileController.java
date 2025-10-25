package com.fptuni.vms.controller;

import com.fptuni.vms.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fptuni.vms.model.User;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.dto.response.ChangePasswordForm;
import com.fptuni.vms.dto.response.ProfileForm;
import com.fptuni.vms.dto.VolunteerRatingDto;
import com.fptuni.vms.dto.ScheduleApplicationDto;
import com.fptuni.vms.dto.VolunteerScheduleResponseDto;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.ApplicationService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private UserService userService;
    private ApplicationService applicationService;

    public ProfileController(UserService userService, ApplicationService applicationService) {
        this.userService = userService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public String viewProfile(Model model, Authentication authentication) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", freshUser);
        model.addAttribute("activePage", "profile");

        if ("Volunteer".equals(freshUser.getRole().getRoleName())) {
            VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
            model.addAttribute("volunteerRating", rating);
        }

        return "profile/view";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Authentication authentication) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        ProfileForm profileForm = new ProfileForm();
        profileForm.setFullName(freshUser.getFullName());
        profileForm.setEmail(freshUser.getEmail());
        profileForm.setPhone(freshUser.getPhone());
        profileForm.setAddress(freshUser.getAddress());

        model.addAttribute("user", freshUser);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("activePage", "settings");

        if ("Volunteer".equals(freshUser.getRole().getRoleName())) {
            VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
            model.addAttribute("volunteerRating", rating);
        }

        return "profile/edit";
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Valid @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập đã hết hạn");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Update user information
            userService.updateProfile(currentUser.getUserId(), profileForm, avatarFile);

            response.put("success", true);
            response.put("message", "Cập nhật thông tin cá nhân thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật thông tin: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update-form")
    public String updateProfileForm(
            @Valid @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", profileForm);

                if ("Volunteer".equals(currentUser.getRole().getRoleName())) {
                    VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
                    model.addAttribute("volunteerRating", rating);
                }

                return "profile/edit";
            }

            // Update user information
            userService.updateProfile(currentUser.getUserId(), profileForm, avatarFile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công");
            return "redirect:/profile";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật thông tin: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    @PostMapping("/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @ModelAttribute ChangePasswordForm changePasswordForm,
            BindingResult bindingResult,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập đã hết hạn");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Additional validation for password confirmation
            if (!changePasswordForm.isPasswordsMatch()) {
                Map<String, String> errors = new HashMap<>();
                errors.put("confirmPassword", "Mật khẩu xác nhận không khớp");
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Change password
            userService.changePassword(currentUser.getUserId(), changePasswordForm);

            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-schedule")
    public String mySchedule(Model model, Authentication authentication) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        // Check if user is volunteer
        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "redirect:/";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        // Get volunteer's applications
        List<Application> allApplications = applicationService.listMyApplications(currentUser.getUserId());

        // Separate upcoming and past events based on opportunity time
        LocalDateTime now = LocalDateTime.now();
        List<ScheduleApplicationDto> upcomingApplications = allApplications.stream()
                .filter(app -> app.getOpportunity().getStartTime().isAfter(now))
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED)
                .map(this::convertToScheduleDto)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        List<ScheduleApplicationDto> pastApplications = allApplications.stream()
                .filter(app -> app.getOpportunity().getEndTime().isBefore(now))
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED ||
                        app.getStatus() == Application.ApplicationStatus.COMPLETED)
                .map(this::convertToScheduleDto)
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toList());

        // Calculate total hours
        long totalHours = allApplications.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED ||
                        app.getStatus() == Application.ApplicationStatus.COMPLETED)
                .mapToLong(app -> {
                    LocalDateTime start = app.getOpportunity().getStartTime();
                    LocalDateTime end = app.getOpportunity().getEndTime();
                    return java.time.Duration.between(start, end).toHours();
                })
                .sum();

        // Create response DTO
        VolunteerScheduleResponseDto scheduleResponse = new VolunteerScheduleResponseDto();
        scheduleResponse.setUpcomingApplications(upcomingApplications);
        scheduleResponse.setPastApplications(pastApplications);
        scheduleResponse.setUpcomingCount(upcomingApplications.size());
        scheduleResponse.setCompletedCount(pastApplications.size());
        scheduleResponse.setTotalHours(totalHours);

        model.addAttribute("user", freshUser);
        model.addAttribute("scheduleData", scheduleResponse);
        model.addAttribute("activePage", "schedule");

        return "volunteer/my-schedual";
    }

    private ScheduleApplicationDto convertToScheduleDto(Application app) {
        ScheduleApplicationDto dto = new ScheduleApplicationDto();
        dto.setAppId(app.getAppId());
        dto.setOpportunityTitle(app.getOpportunity().getTitle());
        dto.setOrganizationName(app.getOpportunity().getOrganization().getName());
        dto.setLocation(app.getOpportunity().getLocation());
        dto.setStartTime(app.getOpportunity().getStartTime());
        dto.setEndTime(app.getOpportunity().getEndTime());
        dto.setAppliedAt(app.getAppliedAt());
        dto.setStatus(app.getStatus().name());
        dto.setNeededVolunteers(app.getOpportunity().getNeededVolunteers());
        dto.setDescription(app.getOpportunity().getSubtitle());
        dto.setThumbnailUrl(app.getOpportunity().getThumbnailUrl());
        return dto;
    }

}