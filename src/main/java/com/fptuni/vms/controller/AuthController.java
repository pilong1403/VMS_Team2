package com.fptuni.vms.controller;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {

    private final AuthService auth;
    private final OrganizationRepository orgRepo;

    public AuthController(AuthService auth, OrganizationRepository orgRepo) {
        this.auth = auth;
        this.orgRepo = orgRepo;
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
            // 1) Xác thực
            User u = auth.login(email, password);

            // 2) Tạo Authentication
            var principal = new com.fptuni.vms.security.CustomUserDetails(u);
            var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            // 3) Tạo SecurityContext và GẮN vào Holder
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);

            // 4) ĐỔI SESSION ID (chống session fixation)
            req.changeSessionId();

            // 5) LƯU SecurityContext vào session (BẮT BUỘC khi tự login trong controller)
            var repo = new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
            repo.saveContext(context, req, resp);

            // 6) (tuỳ chọn) session app-specific
            HttpSession ss = req.getSession(false); // đã tồn tại sau changeSessionId
            if (ss != null) {
                ss.setAttribute("AUTH_USER_ID", u.getUserId());
                ss.setAttribute("AUTH_USER_NAME", u.getFullName());
                ss.setAttribute("AUTH_ROLE", u.getRole().getRoleName());
                ss.setAttribute("AUTH_USER_AVATAR", u.getAvatarUrl()); // Thêm avatar URL
            }

            // 7) Điều hướng theo role (không sử dụng saved request để tránh DevTools
            // interference)

            // 8) Điều hướng theo role
            String r = u.getRole().getRoleName();
            if ("ADMIN".equals(r))
                return "redirect:/admin/reports";
            if ("ORG_OWNER".equals(r))
                return "redirect:/ratings";
            return "redirect:/home";

        } catch (AuthService.AuthException ex) {
            String code = (ex.getCode() != null && !ex.getCode().isBlank()) ? ex.getCode() : "SYSTEM_ERROR";
            return "redirect:/login?e=" + url(code) + "&email=" + url(email);
        } catch (Exception ex) {
            return "redirect:/login?e=SYSTEM_ERROR&email=" + url(email);
        }
    }

    private String map(String code) {
        if (code == null)
            return null;
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
        try {
            return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
