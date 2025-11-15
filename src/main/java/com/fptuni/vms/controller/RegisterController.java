// src/main/java/com/fptuni/vms/controller/RegisterController.java
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
                           @RequestParam(value = "e", required = false) String e,
                           HttpSession session) {
        if (!model.containsAttribute("form")) {
            RegisterForm form = new RegisterForm();

            // Prefill từ session nếu có PENDING_REG
            RegisterForm pending = (RegisterForm) session.getAttribute("PENDING_REG");
            if (pending != null) {
                form.setFullName(pending.getFullName());
                form.setEmail(pending.getEmail());
                form.setPhone(pending.getPhone());
                // Không bao giờ prefill password
                form.setPassword(null);
                form.setConfirmPassword(null);
            }

            model.addAttribute("form", form);
        }
        model.addAttribute("error", mapError(e));
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(@Valid @ModelAttribute("form") RegisterForm form,
                                 BindingResult binding,
                                 HttpServletRequest req,
                                 Model model) {
        if (binding.hasErrors()) return "auth/register";

        try {
            // Chuẩn hoá email trước khi gửi OTP
            String normalizedEmail = form.getEmail().trim().toLowerCase();

            // 1) Gửi OTP
            otpService.generateAndSendOtp(normalizedEmail, "VERIFY_EMAIL");

            // 2) Lưu form tạm vào session
            HttpSession ss = req.getSession(true);
            form.setEmail(normalizedEmail);
            ss.setAttribute("PENDING_REG", form);

            // 3) Sang trang nhập OTP
            model.addAttribute("email", normalizedEmail);
            return "auth/register-verify";

        } catch (OtpVerificationService.ActiveOtpExistsException ex) {
            model.addAttribute("error", "Bạn đã có mã xác minh còn hiệu lực. Vui lòng thử lại sau ít phút.");
            return "auth/register";

        } catch (OtpVerificationService.MailSendException ex) {
            model.addAttribute("error", "Không thể gửi email xác minh. Vui lòng thử lại hoặc liên hệ quản trị.");
            return "auth/register";

        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "Dữ liệu vi phạm ràng buộc CSDL (ví dụ: email đã dùng hoặc độ dài vượt giới hạn).");
            return "auth/register";

        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("error", "Không thể gửi mã xác minh. Vui lòng thử lại.");
            return "auth/register";
        }
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

        String normalizedEmail = email.trim().toLowerCase();
        if (!form.getEmail().equalsIgnoreCase(normalizedEmail)) {
            model.addAttribute("email", form.getEmail());
            model.addAttribute("error", "Email không trùng với phiên đăng ký đang chờ.");
            return "auth/register-verify";
        }

        try {
            // 1) Xác minh OTP
            otpService.verifyOtp(normalizedEmail, "VERIFY_EMAIL", otp);

            // 2) Tạo tài khoản thực trong DB
            authService.registerVolunteer(
                    form.getFullName(),
                    normalizedEmail,
                    form.getPhone(),
                    form.getPassword()
            );

            // 3) Xóa session tạm
            ss.removeAttribute("PENDING_REG");

            // 4) Thông báo thành công
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công. Vui lòng đăng nhập để tiếp tục.");

            // 5) Chuyển về /login
            return "redirect:/login";

        } catch (OtpVerificationService.OtpException ex) {
            // ex.getMessage() đang là "OTP_INVALID", "OTP_EXPIRED", ...
            model.addAttribute("email", form.getEmail());
            model.addAttribute("error", mapOtpErrorMessage(ex.getMessage()));
            return "auth/register-verify";

        } catch (DataIntegrityViolationException ex) {
            return "redirect:/register?e=DATA_VIOLATION";

        } catch (AuthService.AuthException ex) {
            return "redirect:/register?e=" + url(ex.getMessage());

        } catch (Exception ex) {
            ex.printStackTrace();
            return "redirect:/register?e=SYSTEM_ERROR";
        }
    }
    @PostMapping("/register/resend")
    public String resend(@RequestParam String email,
                         HttpSession ss,
                         Model model) {

        RegisterForm pending = (RegisterForm) ss.getAttribute("PENDING_REG");
        if (ss == null || pending == null) {
            return "redirect:/register?e=SESSION_EXPIRED";
        }

        String normalizedEmail = email.trim().toLowerCase();

        try {
            otpService.generateAndSendOtp(normalizedEmail, "VERIFY_EMAIL");
            // Đưa lại email vào model để view render đúng
            model.addAttribute("email", normalizedEmail);
            return "auth/register-verify";
        } catch (OtpVerificationService.ActiveOtpExistsException ex) {
            model.addAttribute("email", normalizedEmail);
            model.addAttribute("error", "Bạn đã có mã xác minh còn hiệu lực. Vui lòng dùng mã đó hoặc thử lại sau ít phút.");
            return "auth/register-verify";
        } catch (OtpVerificationService.MailSendException ex) {
            model.addAttribute("email", normalizedEmail);
            model.addAttribute("error", "Không thể gửi email xác minh. Vui lòng thử lại hoặc liên hệ quản trị.");
            return "auth/register-verify";
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("email", normalizedEmail);
            model.addAttribute("error", "Không thể gửi lại mã xác minh. Vui lòng thử lại.");
            return "auth/register-verify";
        }
    }

    private String mapError(String code) {
        if (code == null) return null;
        return switch (code) {
            case "SESSION_EXPIRED" -> "Phiên đăng ký đã hết hạn. Vui lòng bắt đầu lại.";
            case "SYSTEM_ERROR" -> "Đăng ký thất bại do lỗi hệ thống. Vui lòng thử lại sau.";
            case "DATA_VIOLATION" -> "Dữ liệu vi phạm ràng buộc CSDL (ví dụ: email đã dùng hoặc độ dài vượt giới hạn).";
            case "REGISTER_OK" -> "Đăng ký thành công. Vui lòng đăng nhập.";
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
    private String mapOtpErrorMessage(String code) {
        if (code == null || code.isBlank()) {
            return "Có lỗi xảy ra, vui lòng thử lại.";
        }

        return switch (code) {
            case "OTP_INVALID" -> "Mã xác nhận không chính xác. Vui lòng kiểm tra lại.";
            case "OTP_EXPIRED" -> "Mã xác nhận đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.";
            case "OTP_NOT_FOUND" -> "Không tìm thấy mã xác nhận phù hợp. Vui lòng kiểm tra email hoặc gửi lại mã.";
            case "OTP_ALREADY_USED" -> "Mã xác nhận này đã được sử dụng. Vui lòng yêu cầu mã mới.";
            case "OTP_ACTIVE_EXISTS" -> "Bạn đã có một mã xác nhận còn hiệu lực. Vui lòng dùng mã đó hoặc thử lại sau ít phút.";
            case "OTP_PURPOSE_INVALID" -> "Mục đích xác thực không hợp lệ. Vui lòng thử lại.";
            default -> "Có lỗi khi xác minh mã xác nhận. Vui lòng thử lại.";
        };
    }

}
