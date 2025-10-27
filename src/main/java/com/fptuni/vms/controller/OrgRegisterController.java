package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OrgRegisterForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.AuthService;
import com.fptuni.vms.service.OtpVerificationService;
import com.fptuni.vms.service.OrganizationService;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.fptuni.vms.constants.OrgRegisterConstants.*;

@Controller
public class OrgRegisterController {

    private final OrganizationService organizationService;
    private final OtpVerificationService otpService;
    private final CloudStorageService cloudStorageService;
    private final AuthService authService;

    public OrgRegisterController(OrganizationService organizationService,
                                 OtpVerificationService otpService,
                                 CloudStorageService cloudStorageService,
                                 AuthService authService) {
        this.organizationService = organizationService;
        this.otpService = otpService;
        this.cloudStorageService = cloudStorageService;
        this.authService = authService;
    }

    private static final String SESSION_FILE_BYTES = "ORG_PENDING_FILE_BYTES";
    private static final String SESSION_FILE_NAME  = "ORG_PENDING_FILE_NAME";
    private static final String SESSION_FILE_TYPE  = "ORG_PENDING_FILE_TYPE";

    // NEW: session keys cho avatar
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

            // tên file cũ (nếu có) để hiển thị nhắc
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

        // luôn có session để mình có thể lưu file tạm dù có lỗi
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
            // nếu file tài liệu hợp lệ -> LƯU NGAY vào session để giữ lại khi form có lỗi khác
            if (regDocOk) {
                try {
                    session.setAttribute(SESSION_FILE_BYTES, file.getBytes());
                    session.setAttribute(SESSION_FILE_NAME, safe(file.getOriginalFilename(), "document"));
                    session.setAttribute(SESSION_FILE_TYPE, file.getContentType());
                    prevName = (String) session.getAttribute(SESSION_FILE_NAME); // cập nhật để render lại
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
            // file avatar hợp lệ -> LƯU NGAY vào session
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
        // nếu không upload mới, vẫn giữ cái đã có trong session (nếu có)

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

            // Lưu toàn bộ form (text) để prefill lại khi quay lại chỉnh sửa từ trang verify
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

        OrgRegisterForm form = (OrgRegisterForm) ss.getAttribute(SESSION_PENDING_ORG);
        byte[] fileBytes     = (byte[]) ss.getAttribute(SESSION_FILE_BYTES);
        String fileName      = (String) ss.getAttribute(SESSION_FILE_NAME);
        String contentType   = (String) ss.getAttribute(SESSION_FILE_TYPE);

        // NEW: avatar từ session
        byte[] avatarBytes   = (byte[]) ss.getAttribute(SESSION_AVATAR_BYTES);
        String avatarName    = (String) ss.getAttribute(SESSION_AVATAR_NAME);
        String avatarType    = (String) ss.getAttribute(SESSION_AVATAR_TYPE);

        try {
            // 1) OTP
            String normalizedEmail = safe(email).trim().toLowerCase();
            otpService.verifyOtp(normalizedEmail, OTP_PURPOSE_ORG_REGISTER, otp);

            // 2) Upload avatar nếu có, lấy URL
            String avatarUrl = null;
            if (avatarBytes != null && avatarBytes.length > 0) {
                MultipartFile avatarInMem = new InMemFile("avatarFile", avatarName, avatarType, avatarBytes);
                avatarUrl = cloudStorageService.uploadFile(avatarInMem); // trả URL
                if (avatarUrl == null) throw new RuntimeException("Upload avatar thất bại.");
            }

            // 3) Tạo user ORG_OWNER (AuthServiceImpl đã set LOCKED)
            User owner = authService.registerOwnerAccount(
                    form.getFullName(),
                    form.getEmail(),
                    form.getPhone(),
                    form.getPassword(),
                    form.getAddress(),
                    avatarUrl // có thể null
            );

            // 4) Upload tài liệu đăng ký
            MultipartFile inMem = new InMemFile("regDocFile", fileName, contentType, fileBytes);
            String regDocUrl = cloudStorageService.uploadFile(inMem);
            if (regDocUrl == null) throw new RuntimeException("Upload tài liệu thất bại.");

            // 5) Insert organizations (PENDING)
            organizationService.submitRegistration(
                    owner,
                    form.getOrgName(),
                    form.getDescription(),
                    regDocUrl,
                    form.getRegNote()
            );

// 6) Clear session
            ss.removeAttribute(SESSION_PENDING_ORG);
            ss.removeAttribute(SESSION_FILE_BYTES);
            ss.removeAttribute(SESSION_FILE_NAME);
            ss.removeAttribute(SESSION_FILE_TYPE);
            ss.removeAttribute(SESSION_AVATAR_BYTES);
            ss.removeAttribute(SESSION_AVATAR_NAME);
            ss.removeAttribute(SESSION_AVATAR_TYPE);


// Flash + redirect về trang login
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

    /** RESEND OTP (POST) */
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
        return a.isAuthenticated() && !(principal instanceof String); // tránh "anonymousUser"
    }
    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static String safe(String s){ return s == null ? "" : s; }
    private static String safe(String s, String def){ return isBlank(s) ? def : s; }

    /** MultipartFile in-memory (không lưu file vào session) */
    private static final class InMemFile implements MultipartFile {
        private final String name, originalFilename, contentType; private final byte[] content;
        InMemFile(String name, String originalFilename, String contentType, byte[] content){
            this.name = name==null?"file":name;
            this.originalFilename = originalFilename==null?"file":originalFilename;
            this.contentType = contentType;
            this.content = content==null?new byte[0]:content;
        }
        @Override public String getName(){ return name; }
        @Override public String getOriginalFilename(){ return originalFilename; }
        @Override public String getContentType(){ return contentType; }
        @Override public boolean isEmpty(){ return content.length==0; }
        @Override public long getSize(){ return content.length; }
        @Override public byte[] getBytes(){ return content.clone(); }
        @Override public InputStream getInputStream(){ return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }

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
