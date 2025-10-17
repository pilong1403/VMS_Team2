// src/main/java/com/fptuni/vms/controller/AuthController.java
package com.fptuni.vms.controller;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository; // <-- thêm
import com.fptuni.vms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {

    private final AuthService auth;
    private final OrganizationRepository orgRepo; // <-- thêm

    public AuthController(AuthService auth, OrganizationRepository orgRepo) {
        this.auth = auth;
        this.orgRepo = orgRepo; // <-- thêm
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
            // 1) Business login của bạn
            User u = auth.login(email, password);

            // 2) Đưa user vào SecurityContext
            var principal = new com.fptuni.vms.security.CustomUserDetails(u);
            var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            // 3) Đảm bảo có HttpSession để persist SecurityContext
            req.getSession(true);

            // 4) (tuỳ chọn) session app-specific
            HttpSession ss = req.getSession();
            ss.setAttribute("AUTH_USER_ID", u.getUserId());
            ss.setAttribute("AUTH_USER_NAME", u.getFullName());
            ss.setAttribute("AUTH_ROLE", u.getRole().getRoleName());

            // 5) Quay lại URL ban đầu nếu có
            var cache = new HttpSessionRequestCache();
            SavedRequest saved = cache.getRequest(req, resp);
            if (saved != null) {
                String targetUrl = saved.getRedirectUrl();
                cache.removeRequest(req, resp);
                return "redirect:" + targetUrl;
            }

            // 6) Fallback theo role
            String r = u.getRole().getRoleName();
            if ("ADMIN".equals(r)) {
                return "redirect:/admin/dashboard?msg=login_ok";
            }
            if ("ORG_OWNER".equals(r)) {
                boolean hasOrg = orgRepo.findByOwnerId(u.getUserId()).isPresent();
                if (!hasOrg) return "redirect:/auth/org-register?msg=please_register_org";
                return "redirect:/opportunity/opps-list?msg=login_ok";
            }

            // VOLUNTEER
            return "redirect:/vol/dashboard?msg=login_ok";

        } catch (AuthService.AuthException ex) {
            return "redirect:/login?e=" + url(ex.getMessage()) + "&email=" + url(email);
        } catch (Exception ex) {
            return "redirect:/login?e=SYSTEM_ERROR&email=" + url(email);
        }
    }

    private String map(String code) {
        if (code == null) return null;
        return switch (code) {
            case "INVALID_CREDENTIALS" -> "Sai email hoặc mật khẩu.";
            case "ACCOUNT_LOCKED" -> "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.";
            case "USERNAME_PASSWORD_REQUIRED" -> "Bạn phải nhập Email và Mật khẩu.";
            case "SYSTEM_ERROR" -> "Đăng nhập thất bại do lỗi hệ thống. Vui lòng thử lại sau.";
            case "LOGOUT_OK" -> "Bạn đã đăng xuất.";
            default -> null;
        };
    }

    private String url(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
        catch (Exception e) { return ""; }
    }
}
