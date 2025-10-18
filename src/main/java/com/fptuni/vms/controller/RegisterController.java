package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.RegisterForm;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.AuthService;
import com.fptuni.vms.service.OtpVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final AuthService authService;
    private final OtpVerificationService otpService;

    public RegisterController(AuthService authService, OtpVerificationService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    /** Trim toàn bộ String input; empty -> null để @NotBlank bắt được */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/register")
    public String showForm(Model model,
                           @RequestParam(value = "e", required = false) String e) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        model.addAttribute("error", mapError(e));
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(@Valid @ModelAttribute("form") RegisterForm form,
                                 BindingResult binding,
                                 HttpServletRequest req,
                                 Model model) {
        // 1) Kiểm tra confirm password
        if (!form.passwordsMatch()) {
            binding.rejectValue("confirmPassword", "Mismatch", "Passwords do not match");
        }
        if (binding.hasErrors()) return "auth/register";

        try {
            // 2) Generate & send OTP
            otpService.generateAndSendOtp(form.getEmail(), "VERIFY_EMAIL");

            // 3) Save pending form to session
            HttpSession ss = req.getSession(true);
            ss.setAttribute("PENDING_REG", form);

            // 4) Go to OTP page
            model.addAttribute("email", form.getEmail());
            return "auth/register-verify";

        } catch (OtpVerificationService.ActiveOtpExistsException ex) {
            // Đã có mã còn hiệu lực
            model.addAttribute("error", "Bạn đã có mã xác minh còn hiệu lực. Vui lòng thử lại sau ít phút.");
            return "auth/register";

        } catch (OtpVerificationService.MailSendException ex) {
            // Lỗi gửi mail (đã được service chuẩn hóa)
            model.addAttribute("error", "Không thể gửi email xác minh. Vui lòng thử lại hoặc liên hệ quản trị.");
            return "auth/register";

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Phòng trường hợp ràng buộc DB
            model.addAttribute("error", "Dữ liệu vi phạm ràng buộc CSDL (email trùng hoặc độ dài).");
            return "auth/register";

        } catch (Exception ex) {
            // Fallback
            ex.printStackTrace();
            model.addAttribute("error", "Cannot send verification code. Please try again.");
            return "auth/register";
        }

    }

    /** Diễn giải nhanh lỗi DB thường gặp để show message dễ hiểu */
    private String guessConstraintMessage(Exception ex) {
        Throwable root = getRootCause(ex);
        String msg = root.getMessage();
        if (msg == null) return null;

        // Ví dụ bắt trigger message
        if (msg.contains("Only one active OTP") || msg.contains("active OTP")) {
            return "Bạn đã có mã xác minh còn hiệu lực. Vui lòng thử lại sau ít phút.";
        }
        // Bắt unique
        if (msg.toLowerCase().contains("unique") || msg.toLowerCase().contains("uq_")) {
            return "Giá trị đã tồn tại (unique).";
        }
        // Bắt tràn cột
        if (msg.toLowerCase().contains("string or binary data would be truncated")) {
            return "Một trường vượt quá độ dài tối đa trong CSDL.";
        }
        return null;
    }

    private Throwable getRootCause(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r;
    }


    @PostMapping("/register/verify")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            HttpServletRequest req,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        HttpSession ss = req.getSession(false);
        if (ss == null || ss.getAttribute("PENDING_REG") == null) {
            return "redirect:/register?e=SESSION_EXPIRED";
        }
        RegisterForm form = (RegisterForm) ss.getAttribute("PENDING_REG");

        if (!form.getEmail().equalsIgnoreCase(email)) {
            model.addAttribute("email", form.getEmail());
            model.addAttribute("error", "Email does not match pending registration");
            return "auth/register-verify";
        }

        try {
            // 1) Verify OTP
            otpService.verifyOtp(email, "VERIFY_EMAIL", otp);

            // 2) Create user
            User created = authService.registerVolunteer(
                    form.getFullName(),   // <= đã @Size(max=100) ở RegisterForm
                    form.getEmail(),      // <= @Size(max=100) + @UniqueEmail
                    form.getPhone(),      // <= @Size(max=20) + regex
                    form.getPassword()
            );

            // 3) Clear session
            ss.removeAttribute("PENDING_REG");

            // 4) Flash message
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful. Please login to continue.");

            // 5) Redirect -> /login
            return "redirect:/login";

        } catch (OtpVerificationService.OtpException ex) {
            model.addAttribute("email", email);
            model.addAttribute("error", ex.getMessage());
            return "auth/register-verify";

        } catch (DataIntegrityViolationException ex) {
            // Bắt lỗi ràng buộc DB (unique/length) phòng khi có race condition
            return "redirect:/register?e=DATA_VIOLATION";

        } catch (AuthService.AuthException ex) {
            return "redirect:/register?e=" + url(ex.getMessage());

        } catch (Exception ex) {
            return "redirect:/register?e=SYSTEM_ERROR";
        }
    }

    private String mapError(String code) {
        if (code == null) return null;
        return switch (code) {
            case "SESSION_EXPIRED" -> "Your registration session has expired. Please start again.";
            case "SYSTEM_ERROR" -> "Registration failed due to a system error. Please try again later.";
            case "DATA_VIOLATION" -> "Your data violates database constraints (e.g., email already used or value too long).";
            // "REGISTER_OK" không còn dùng nữa, vẫn map nếu bị gọi cũ
            case "REGISTER_OK" -> "Registration successful. Please login.";
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
