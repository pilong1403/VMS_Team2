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
import com.fptuni.vms.dto.ChangePasswordForm;
import com.fptuni.vms.dto.ProfileForm;
import com.fptuni.vms.dto.VolunteerRatingDto;
import com.fptuni.vms.service.UserService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
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

}