// src/main/java/com/fptuni/vms/controller/PasswordResetController.java
package com.fptuni.vms.controller;

import com.fptuni.vms.model.User;
import com.fptuni.vms.service.OtpVerificationService;
import com.fptuni.vms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private static final String SESSION_RESET_EMAIL = "RESET_EMAIL";

    private final OtpVerificationService otpService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(OtpVerificationService otpService,
                                   UserService userService,
                                   PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // STEP 1: Nhập email (render)
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(
            @ModelAttribute(value = "prefillEmail") String prefillEmail,
            @ModelAttribute(value = "errorCode") String errorCode,
            Model model) {

        // Khi không có flash attribute, hai tham số trên sẽ là chuỗi rỗng -> chuẩn hóa về null
        if (prefillEmail != null && prefillEmail.isBlank()) prefillEmail = null;
        if (errorCode != null && errorCode.isBlank()) errorCode = null;

        model.addAttribute("prefillEmail", prefillEmail);
        model.addAttribute("error", map(errorCode));
        return "auth/forgot-password";
    }

    // STEP 1: Nhập email (xử lý)
    @PostMapping("/forgot-password")
    public String handleForgot(@RequestParam("email") String email,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {

        if (email == null || email.isBlank()) {
            redirectAttrs.addFlashAttribute("prefillEmail", email);
            redirectAttrs.addFlashAttribute("errorCode", "EMAIL_REQUIRED");
            return "redirect:/forgot-password";
        }
        email = email.trim().toLowerCase();

        boolean exists = userService.existsByEmail(email);
        if (!exists) {
            // Không gửi OTP, chỉ báo lỗi + prefill
            redirectAttrs.addFlashAttribute("prefillEmail", email);
            redirectAttrs.addFlashAttribute("errorCode", "EMAIL_NOT_REGISTERED");
            return "redirect:/forgot-password";
        }

        try {
            otpService.generateAndSendOtp(email, "RESET_PASSWORD");
            // Chỉ khi email hợp lệ và gửi OTP thành công mới ghi vào session cho bước verify/reset
            session.setAttribute(SESSION_RESET_EMAIL, email);
            return "redirect:/forgot-password/verify";
        } catch (OtpVerificationService.ActiveOtpExistsException ex) {
            session.setAttribute(SESSION_RESET_EMAIL, email);
            return "redirect:/forgot-password/verify?e=OTP_ACTIVE";
        } catch (OtpVerificationService.MailSendException ex) {
            redirectAttrs.addFlashAttribute("prefillEmail", email);
            redirectAttrs.addFlashAttribute("errorCode", "MAIL_FAIL");
            return "redirect:/forgot-password";
        } catch (RuntimeException ex) {
            redirectAttrs.addFlashAttribute("prefillEmail", email);
            redirectAttrs.addFlashAttribute("errorCode", "SYSTEM_ERROR");
            return "redirect:/forgot-password";
        }
    }

    // STEP 2: Nhập OTP
    @GetMapping("/forgot-password/verify")
    public String verifyPage(@RequestParam(value = "e", required = false) String e,
                             HttpSession session, Model model) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("email", email);
        model.addAttribute("error", map(e));
        return "auth/forgot-verify";
    }

    @PostMapping("/forgot-password/verify")
    public String handleVerify(@RequestParam("code") String code, HttpSession session) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";
        try {
            otpService.verifyOtp(email, "RESET_PASSWORD", code);
            return "redirect:/forgot-password/reset";
        } catch (OtpVerificationService.OtpException ex) {
            return "redirect:/forgot-password/verify?e=" + ex.getMessage();
        } catch (RuntimeException ex) {
            return "redirect:/forgot-password/verify?e=SYSTEM_ERROR";
        }
    }

    // STEP 3: Đặt mật khẩu mới
    @GetMapping("/forgot-password/reset")
    public String resetPage(@RequestParam(value = "e", required = false) String e,
                            HttpSession session, Model model) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("email", email);
        model.addAttribute("error", map(e));
        return "auth/reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String handleReset(@RequestParam("password") String password,
                              @RequestParam("confirm") String confirm,
                              HttpSession session) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";

        if (password == null || password.isBlank() || !password.equals(confirm)) {
            return "redirect:/forgot-password/reset?e=CONFIRM_MISMATCH";
        }

        User u = userService.findByEmail(email).orElse(null);
        if (u != null) {
            u.setPasswordHash(passwordEncoder.encode(password));
            userService.save(u);
        }
        session.removeAttribute(SESSION_RESET_EMAIL);
        return "redirect:/login?e=RESET_OK&email=" + url(email);
    }

    // ===== util =====
    private String map(String code) {
        if (code == null) return null;
        return switch (code) {
            case "EMAIL_REQUIRED" -> "Vui lòng nhập email để tiếp tục.";
            case "EMAIL_NOT_REGISTERED" -> "Email này chưa được đăng ký tài khoản. Vui lòng kiểm tra lại.";
            case "OTP_ACTIVE" -> "Mã OTP đã được gửi trước đó, vui lòng kiểm tra email của bạn.";
            case "MAIL_FAIL" -> "Gửi email thất bại. Vui lòng thử lại sau.";
            case "OTP_NOT_FOUND" -> "Không tìm thấy mã OTP. Hãy yêu cầu gửi lại.";
            case "OTP_ALREADY_USED" -> "Mã OTP đã được sử dụng.";
            case "OTP_EXPIRED" -> "Mã OTP đã hết hạn. Hãy yêu cầu gửi mã mới.";
            case "OTP_INVALID" -> "Mã OTP không đúng.";
            case "CONFIRM_MISMATCH" -> "Mật khẩu xác nhận không khớp.";
            case "SYSTEM_ERROR" -> "Có lỗi hệ thống. Vui lòng thử lại sau.";
            default -> null;
        };
    }

    private String url(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
