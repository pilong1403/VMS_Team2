package com.fptuni.vms.config;

import com.fptuni.vms.security.CustomOidcUserService;
import com.fptuni.vms.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http,
                                        CustomOidcUserService customOidcUserService,   // <-- inject bean
                                        OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) throws Exception {


                // Trang 403 tuỳ biến
                AccessDeniedHandlerImpl denied = new AccessDeniedHandlerImpl();
                denied.setErrorPage("/403");

                // Cấu hình CSRF: lưu token trong session và expose vào request attributes để
                // Thymeleaf đọc được
                HttpSessionCsrfTokenRepository csrfRepo = new HttpSessionCsrfTokenRepository();
                csrfRepo.setSessionAttributeName("_csrf"); // mặc định đã là _csrf, đặt rõ cho chắc
                CsrfTokenRequestAttributeHandler csrfAttr = new CsrfTokenRequestAttributeHandler();
                csrfAttr.setCsrfRequestAttributeName("_csrf");

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(csrfRepo)
                                                .csrfTokenRequestHandler(csrfAttr))

                                // Không dùng login form mặc định
                                .formLogin(form -> form.disable())

                                // Tắt request cache để tránh redirect không mong muốn từ DevTools
                                .requestCache(cache -> cache.disable())

                                // Khi chưa đăng nhập, chuyển tới /login; khi bị cấm, tới /403
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new LoginUrlAuthenticationEntryPoint("/login"))
                                                .accessDeniedHandler(denied))

                                // Logout
                                .logout(l -> l
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?e=LOGOUT_OK"))

                                // Phân quyền/cho phép
                                .authorizeHttpRequests(auth -> auth
                                                // PUBLIC (không cần đăng nhập)
                                                .requestMatchers(
                                                                "/", "/home", "/opportunities", "/opportunities/**",
                                                                "/about",
                                                                "/login", "/403",
                                                                "/oauth2/**", "/login/oauth2/**",
                                                                "/register", "/register/**",
                                                                "/auth/org-register", "/auth/org-register/**",
                                                                "/assets/**", "/css/**", "/js/**", "/images/**",
                                                                "/forgot-password", "/forgot-password/**",
                                                                "/webjars/**", "/favicon.ico", "/faqPublic",
                                                                "/organizations/**")

                                                .permitAll()
                                                .requestMatchers("/org/register", "/org/register/**").anonymous()
                                                // ADMIN
                                                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                                                .requestMatchers("/organization/**").hasAuthority("ORG_OWNER")

                                                // Điều hướng sau đăng nhập theo vai trò (ví dụ)
                                                .requestMatchers("/home")
                                                .hasAnyAuthority("VOLUNTEER", "ADMIN", "ORG_OWNER")

                                                // Chức năng dành cho chủ tổ chức
                                                .requestMatchers("/opportunity/**").hasAuthority("ORG_OWNER")
                                                .requestMatchers("/org/opps/**").hasAuthority("ORG_OWNER")
                                                // Vùng tự phục vụ
                                                .requestMatchers("/vol/**")
                                                .hasAnyAuthority("VOLUNTEER", "ORG_OWNER", "ADMIN")

                                                // Volunteer specific paths
                                                .requestMatchers("/volunteer/**")
                                                .hasAnyAuthority("VOLUNTEER", "ADMIN")

                                                // Các URL còn lại yêu cầu đăng nhập
                                                .anyRequest().authenticated());
                                                    http.oauth2Login(oauth -> oauth
                                                                    .loginPage("/login")
                                                                    .userInfoEndpoint(ui -> ui.oidcUserService(customOidcUserService))
                                                                    .successHandler(oAuth2LoginSuccessHandler)  // <-- dùng đúng biến đã inject
                                                            .failureUrl("/login?e=SYSTEM_ERROR")
                                                    );

                                            http.oauth2Login(oauth -> oauth
                                                    .loginPage("/login")
                                                    .userInfoEndpoint(ui -> ui.oidcUserService(customOidcUserService)) // <-- OIDC
                                                    .successHandler(oAuth2LoginSuccessHandler)
                                                    .failureHandler((req, resp, ex) -> {
                                                        String code = "SYSTEM_ERROR";
                                                        if (ex instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oae
                                                                && "oauth_role_blocked".equalsIgnoreCase(oae.getError().getErrorCode())) {
                                                            code = "OAUTH_ROLE_BLOCKED";
                                                        }
                                                        resp.sendRedirect("/login?e=" + code);
                                                    })
                                            );

            return http.build();
        }
}
