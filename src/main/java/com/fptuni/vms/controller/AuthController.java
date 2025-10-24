package com.fptuni.vms.controller;

import com.fptuni.vms.enums.AuthErrorCode;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "e", required = false) String e,
                        @RequestParam(value = "email", required = false) String email,
                        Model model) {
        model.addAttribute("error", map(e));
        model.addAttribute("email", email);
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletRequest req,
                          HttpServletResponse resp) {
        try {
            // 1) Xác thực qua service
            User u = auth.login(email, password);

            // 2) Tạo Authentication
            var principal = new com.fptuni.vms.security.CustomUserDetails(u);
            var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            // 3) Tạo SecurityContext và set vào Holder
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);

            // 4) Đổi session id (chống session fixation)
            req.changeSessionId();

            // 5) Lưu SecurityContext vào session
            var repo = new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
            repo.saveContext(context, req, resp);

            // 6) Session app-specific
            HttpSession ss = req.getSession(false);
            if (ss != null) {
                ss.setAttribute("AUTH_USER_ID", u.getUserId());
                ss.setAttribute("AUTH_USER_NAME", u.getFullName());
                ss.setAttribute("AUTH_ROLE", u.getRole().getRoleName());
                ss.setAttribute("AUTH_USER_AVATAR", u.getAvatarUrl());
            }

            // 7) Điều hướng theo role
            String r = u.getRole().getRoleName();
            if ("ADMIN".equals(r))
                return "redirect:/admin/reports";
            if ("ORG_OWNER".equals(r))
                return "redirect:/organization/ratings/opportunities";
            return "redirect:/home";

        } catch (AuthService.AuthException ex) {
            // Lấy code enum -> chuỗi; fallback SYSTEM_ERROR nếu null
            String code = (ex.getCode() != null)
                    ? ex.getCode().name()
                    : AuthErrorCode.SYSTEM_ERROR.name();
            return "redirect:/login?e=" + url(code) + "&email=" + url(email);
        } catch (Exception ex) {
            return "redirect:/login?e=" + url(AuthErrorCode.SYSTEM_ERROR.name()) + "&email=" + url(email);
        }
    }

    // Map mã lỗi -> thông điệp hiển thị
// src/main/java/com/fptuni/vms/controller/AuthController.java
    private String map(String code) {
        if (code == null) return null;
        switch (code) {
            case "INVALID_CREDENTIALS":
                return "Sai email hoặc mật khẩu.";
            case "ACCOUNT_LOCKED":
                return "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.";
            case "USERNAME_PASSWORD_REQUIRED":
                return "Bạn phải nhập Email và Mật khẩu.";
            case "SYSTEM_ERROR":
                return "Đăng nhập thất bại do lỗi hệ thống. Vui lòng thử lại sau.";
            case "LOGOUT_OK":
                return "Bạn đã đăng xuất.";
            case "RESET_OK":
                return "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.";
            case "ORG_PENDING":
                return "Tài khoản của bạn chưa thể đăng nhập vì hồ sơ tổ chức đang chờ duyệt.";
            case "ORG_REJECTED":
                return "Hồ sơ tổ chức của bạn đã bị từ chối. Vui lòng đăng ký lại.";

            default:
                return null;
        }
    }


    private String url(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
