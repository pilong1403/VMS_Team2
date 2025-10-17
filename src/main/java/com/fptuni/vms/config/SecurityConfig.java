package com.fptuni.vms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 403 -> forward nội bộ đến /403
        AccessDeniedHandlerImpl denied = new AccessDeniedHandlerImpl();
        denied.setErrorPage("/403");

        http
                // tự làm trang /login, không dùng formLogin mặc định
                .formLogin(form -> form.disable())
                // lưu URL gốc để redirect lại sau login
                .requestCache(cache -> cache.requestCache(new HttpSessionRequestCache()))
                .exceptionHandling(ex -> ex
                        // 401 -> redirect /login
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                        // 403 -> forward /403 (URL giữ nguyên)
                        .accessDeniedHandler(denied)
                )
                .logout(l -> l
                        .logoutUrl("/logout") // POST /logout (có CSRF token)
                        .logoutSuccessUrl("/login?e=LOGOUT_OK")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/403",
                                "/register", "/register/**",
                                "/auth/org-register", "/auth/org-register/**",
                                "/assets/**","/css/**","/js/**","/images/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/org/**").hasAuthority("ORG_OWNER")
                        //  THÊM DÒNG NÀY để cho phép các trang opportunity/* cho ORG_OWNER
                        .requestMatchers("/opportunity/**").hasAuthority("ORG_OWNER")
                        .requestMatchers("/vol/**").hasAnyAuthority("VOLUNTEER","ORG_OWNER","ADMIN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
