package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OrgRegisterForm;
import com.fptuni.vms.service.AuthService;
import com.fptuni.vms.service.OrgRegisterBusinessService;
import com.fptuni.vms.service.OtpVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

import static com.fptuni.vms.constants.OrgRegisterConstants.*;

@Controller
public class OrgRegisterController {

    private final OtpVerificationService otpService;
    private final AuthService authService;
    private final OrgRegisterBusinessService orgRegisterBusinessService;

    public OrgRegisterController(OtpVerificationService otpService,
                                 AuthService authService,
                                 OrgRegisterBusinessService orgRegisterBusinessService) {
        this.otpService = otpService;
        this.authService = authService;
        this.orgRegisterBusinessService = orgRegisterBusinessService;
    }

    private static final String SESSION_FILE_BYTES = "ORG_PENDING_FILE_BYTES";
    private static final String SESSION_FILE_NAME  = "ORG_PENDING_FILE_NAME";
    private static final String SESSION_FILE_TYPE  = "ORG_PENDING_FILE_TYPE";

    // avatar
    private static final String SESSION_AVATAR_BYTES = "ORG_PENDING_AVATAR_BYTES";
    private static final String SESSION_AVATAR_NAME  = "ORG_PENDING_AVATAR_NAME";
    private static final String SESSION_AVATAR_TYPE  = "ORG_PENDING_AVATAR_TYPE";

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true)); // trim all
    }

    @GetMapping("/org/register")
    public String showForm(Model model,
                           @RequestParam(value = "e", required = false, defaultValue = "") String e,
                           HttpSession session) {
        if (isAuthenticated()) {
            return "redirect:/login?e=" + E_MUST_LOGOUT;
        }
        if (!model.containsAttribute("form")) {
            OrgRegisterForm form = new OrgRegisterForm();

            // Prefill từ phiên đang chờ (nếu có)
            OrgRegisterForm pending = (OrgRegisterForm) session.getAttribute(SESSION_PENDING_ORG);
            if (pending != null) {
                form.setOrgName(pending.getOrgName());
                form.setDescription(pending.getDescription());
                form.setRegNote(pending.getRegNote());
                form.setFullName(pending.getFullName());
                form.setEmail(pending.getEmail());
                form.setPhone(pending.getPhone());
                form.setAddress(pending.getAddress());
                // không prefill mật khẩu
                form.setPassword(null);
                form.setConfirmPassword(null);
            }
            model.addAttribute("form", form);

            model.addAttribute("existingDocName", session.getAttribute(SESSION_FILE_NAME));
            model.addAttribute("existingAvatarName", session.getAttribute(SESSION_AVATAR_NAME));
        }
        model.addAttribute(ATTR_ERROR, mapError(e));
        return VIEW_ORG_REGISTER;
    }

    @PostMapping("/org/register")
    public String submit(@Valid @ModelAttribute("form") OrgRegisterForm form,
                         BindingResult binding,
                         HttpServletRequest req,
                         Model model) {
        if (isAuthenticated()) return "redirect:/login?e=" + E_MUST_LOGOUT;

        HttpSession session = req.getSession(true);

        byte[] prevBytes = (byte[]) session.getAttribute(SESSION_FILE_BYTES);
        String prevName  = (String)  session.getAttribute(SESSION_FILE_NAME);
        String prevType  = (String)  session.getAttribute(SESSION_FILE_TYPE);

        // ===== 1) Validate regDocFile — chấp nhận dùng file cũ nếu không chọn mới
        MultipartFile file = form.getRegDocFile();
        boolean hasNewUpload = (file != null && !file.isEmpty());
        boolean regDocOk = true;

        if (!hasNewUpload) {
            if (prevBytes == null || prevBytes.length == 0) {
                binding.rejectValue("regDocFile", "NotBlank", "Vui lòng tải lên tài liệu đăng ký.");
                regDocOk = false;
            }
        } else {
            if (file.getSize() > 5 * 1024 * 1024) {
                binding.rejectValue("regDocFile", "Size", "Tài liệu tối đa 5MB.");
                regDocOk = false;
            }
            String ct = (file.getContentType() == null ? "" : file.getContentType().toLowerCase());
            boolean ok = ct.contains("pdf") || ct.contains("msword") || ct.contains("officedocument") || ct.startsWith("image/");
            if (!ok) {
                binding.rejectValue("regDocFile", "Type", "Chỉ chấp nhận PDF/Word/Ảnh.");
                regDocOk = false;
            }
            if (regDocOk) {
                try {
                    session.setAttribute(SESSION_FILE_BYTES, file.getBytes());
                    session.setAttribute(SESSION_FILE_NAME, safe(file.getOriginalFilename(), "document"));
                    session.setAttribute(SESSION_FILE_TYPE, file.getContentType());
                    prevName = (String) session.getAttribute(SESSION_FILE_NAME);
                } catch (IOException e) {
                    binding.rejectValue("regDocFile", "IO", "Không đọc được file tải lên.");
                    regDocOk = false;
                }
            }
        }

        // ===== 1.x) Validate avatarFile (tùy chọn) — lưu vào session nếu hợp lệ
        MultipartFile avatar = form.getAvatarFile();
        boolean hasAvatarUpload = (avatar != null && !avatar.isEmpty());
        boolean avatarOk = true;

        if (hasAvatarUpload) {
            if (avatar.getSize() > 2 * 1024 * 1024) {
                binding.rejectValue("avatarFile", "Size", "Ảnh tối đa 2MB.");
                avatarOk = false;
            }
            String act = (avatar.getContentType() == null ? "" : avatar.getContentType().toLowerCase());
            if (!act.startsWith("image/")) {
                binding.rejectValue("avatarFile", "Type", "Chỉ chấp nhận định dạng ảnh.");
                avatarOk = false;
            }
            if (avatarOk) {
                try {
                    session.setAttribute(SESSION_AVATAR_BYTES, avatar.getBytes());
                    session.setAttribute(SESSION_AVATAR_NAME, safe(avatar.getOriginalFilename(), "avatar"));
                    session.setAttribute(SESSION_AVATAR_TYPE, avatar.getContentType());
                } catch (IOException e) {
                    binding.rejectValue("avatarFile", "IO", "Không đọc được ảnh tải lên.");
                    avatarOk = false;
                }
            }
        }

        // ===== 2) Confirm password
        if (!binding.hasFieldErrors("password") && !binding.hasFieldErrors("confirmPassword")) {
            if (!safe(form.getPassword()).equals(safe(form.getConfirmPassword()))) {
                binding.rejectValue("confirmPassword", "Mismatch", "Mật khẩu xác nhận không khớp.");
            }
        }

        // ===== 3) Kiểm tra email ở service
        String emailForOtp = safe(form.getEmail()).trim().toLowerCase();
        try {
            authService.assertNewAccountEmailUsable(emailForOtp);
        } catch (AuthService.AuthException ex) {
            switch (ex.getCode()) {
                case INVALID_EMAIL, EMAIL_EXISTS -> binding.rejectValue("email", ex.getCode().name(), ex.getMessage());
                default -> model.addAttribute(ATTR_ERROR, ex.getMessage());
            }
        }

        // ===== 4) Nếu có lỗi -> hiển thị lại + gợi ý file/avatar đã lưu trong session
        if (binding.hasErrors()) {
            model.addAttribute("existingDocName", session.getAttribute(SESSION_FILE_NAME));
            model.addAttribute("existingAvatarName", session.getAttribute(SESSION_AVATAR_NAME));
            return VIEW_ORG_REGISTER;
        }

        // ===== 5) Không lỗi -> gửi OTP + (file/avatar đã lưu sẵn trong session)
        try {
            otpService.generateAndSendOtp(emailForOtp, OTP_PURPOSE_ORG_REGISTER);

            session.setAttribute(SESSION_PENDING_ORG, form);

            model.addAttribute(ATTR_EMAIL, emailForOtp);
            return VIEW_ORG_VERIFY;

        } catch (OtpVerificationService.ActiveOtpExistsException ex) {
            model.addAttribute(ATTR_ERROR, "Bạn đã có mã xác minh còn hiệu lực. Vui lòng thử lại sau ít phút.");
            model.addAttribute("existingDocName", session.getAttribute(SESSION_FILE_NAME));
            model.addAttribute("existingAvatarName", session.getAttribute(SESSION_AVATAR_NAME));
            return VIEW_ORG_REGISTER;
        } catch (OtpVerificationService.MailSendException ex) {
            model.addAttribute(ATTR_ERROR, "Không thể gửi email xác minh. Vui lòng thử lại hoặc liên hệ quản trị.");
            model.addAttribute("existingDocName", session.getAttribute(SESSION_FILE_NAME));
            model.addAttribute("existingAvatarName", session.getAttribute(SESSION_AVATAR_NAME));
            return VIEW_ORG_REGISTER;
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute(ATTR_ERROR, "Không thể gửi mã xác minh. Vui lòng thử lại.");
            model.addAttribute("existingDocName", session.getAttribute(SESSION_FILE_NAME));
            model.addAttribute("existingAvatarName", session.getAttribute(SESSION_AVATAR_NAME));
            return VIEW_ORG_REGISTER;
        }
    }

    @PostMapping("/org/register/verify")
    public String verify(@RequestParam String email,
                         @RequestParam String otp,
                         HttpServletRequest req,
                         Model model,
                         RedirectAttributes ra) {
        if (isAuthenticated()) {
            return "redirect:/login?e=" + E_MUST_LOGOUT;
        }

        HttpSession ss = req.getSession(false);
        if (ss == null || ss.getAttribute(SESSION_PENDING_ORG) == null) {
            return "redirect:/org/register?e=" + E_SESSION_EXPIRED;
        }

        OrgRegisterForm form   = (OrgRegisterForm) ss.getAttribute(SESSION_PENDING_ORG);
        byte[] fileBytes       = (byte[]) ss.getAttribute(SESSION_FILE_BYTES);
        String fileName        = (String) ss.getAttribute(SESSION_FILE_NAME);
        String contentType     = (String) ss.getAttribute(SESSION_FILE_TYPE);

        byte[] avatarBytes     = (byte[]) ss.getAttribute(SESSION_AVATAR_BYTES);
        String avatarName      = (String) ss.getAttribute(SESSION_AVATAR_NAME);
        String avatarType      = (String) ss.getAttribute(SESSION_AVATAR_TYPE);

        try {
            // 1) OTP
            String normalizedEmail = safe(email).trim().toLowerCase();
            otpService.verifyOtp(normalizedEmail, OTP_PURPOSE_ORG_REGISTER, otp);

            // 2–5) Luồng nghiệp vụ chính ở service
            orgRegisterBusinessService.finalizeRegistration(
                    form,
                    fileBytes, fileName, contentType,
                    avatarBytes, avatarName, avatarType
            );

            // 6) Clear session
            ss.removeAttribute(SESSION_PENDING_ORG);
            ss.removeAttribute(SESSION_FILE_BYTES);
            ss.removeAttribute(SESSION_FILE_NAME);
            ss.removeAttribute(SESSION_FILE_TYPE);
            ss.removeAttribute(SESSION_AVATAR_BYTES);
            ss.removeAttribute(SESSION_AVATAR_NAME);
            ss.removeAttribute(SESSION_AVATAR_TYPE);

            ra.addFlashAttribute("success",
                    "Hồ sơ đăng ký đã được gửi thành công. Quản trị viên sẽ xem xét và thông báo kết quả qua email đăng ký.");
            return "redirect:/login";

        } catch (OtpVerificationService.OtpException ex) {
            model.addAttribute(ATTR_EMAIL, email);
            model.addAttribute(ATTR_ERROR, ex.getMessage());
            return VIEW_ORG_VERIFY;
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute(ATTR_EMAIL, email);
            model.addAttribute(ATTR_ERROR, "Lỗi khi xác minh/gửi hồ sơ: " + ex.getMessage());
            return VIEW_ORG_VERIFY;
        }
    }

    @PostMapping("/org/register/resend")
    @ResponseBody
    public String resend(@RequestParam String email, HttpSession ss) {
        if (isAuthenticated()) {
            throw new IllegalStateException(E_MUST_LOGOUT);
        }
        if (ss == null || ss.getAttribute(SESSION_PENDING_ORG) == null) {
            throw new IllegalStateException(E_SESSION_EXPIRED);
        }
        String normalizedEmail = safe(email).trim().toLowerCase();
        otpService.generateAndSendOtp(normalizedEmail, OTP_PURPOSE_ORG_REGISTER);
        return "OK";
    }

    // ===== Helpers =====
    private boolean isAuthenticated() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        Object principal = a.getPrincipal();
        return a.isAuthenticated() && !(principal instanceof String);
    }

    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static String safe(String s){ return s == null ? "" : s; }
    private static String safe(String s, String def){ return isBlank(s) ? def : s; }

    private String mapError(String code) {
        if (code == null || code.isBlank()) return null;
        return switch (code) {
            case E_SESSION_EXPIRED -> "Phiên đăng ký đã hết hạn, vui lòng thực hiện lại.";
            case E_SYSTEM_ERROR    -> "Có lỗi hệ thống. Vui lòng thử lại sau.";
            case E_MUST_LOGOUT     -> "Vui lòng đăng xuất để đăng ký Tổ chức.";
            default -> null;
        };
    }
}
