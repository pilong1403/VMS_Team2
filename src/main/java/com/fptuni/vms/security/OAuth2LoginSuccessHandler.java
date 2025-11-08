package com.fptuni.vms.security;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepo;

    public OAuth2LoginSuccessHandler(UserRepository userRepo) { this.userRepo = userRepo; }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        try {
            // 1) Lấy email từ Google
            OAuth2User oauth = (OAuth2User) authentication.getPrincipal();
            String email = String.valueOf(oauth.getAttributes().get("email")).toLowerCase();

            // 2) Lấy user kèm role
            User u = userRepo.findByEmailWithRole(email).orElse(null);
            if (u == null) {
                response.sendRedirect("/login?e=SYSTEM_ERROR");
                return;
            }

            // 3) Tạo CustomUserDetails và Authentication mới
            var principal = new com.fptuni.vms.security.CustomUserDetails(u);
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );

            // 4) Ghi SecurityContext + session (rất quan trọng)
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);

            request.changeSessionId(); // chống session fixation
            new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                    .saveContext(context, request, response);

            // 5) Biến session cho Thymeleaf
            HttpSession ss = request.getSession(false);
            if (ss != null) {
                ss.setAttribute("AUTH_USER_ID", u.getUserId());
                ss.setAttribute("AUTH_USER_NAME", u.getFullName());
                ss.setAttribute("AUTH_ROLE", u.getRole().getRoleName());
                ss.setAttribute("AUTH_USER_AVATAR", u.getAvatarUrl());
            }

            // 6) Điều hướng theo role
            String r = u.getRole().getRoleName();
            if ("ADMIN".equals(r)) {
                response.sendRedirect("/admin/reports");
            } else if ("ORG_OWNER".equals(r)) {
                response.sendRedirect("/org/opps");
            } else {
                response.sendRedirect("/home");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try { response.sendRedirect("/login?e=SYSTEM_ERROR"); } catch (Exception ignore) {}
        }
    }

}

