package com.fptuni.vms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.web.SecurityFilterChain;

import com.fptuni.vms.security.JpaUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        private final JpaUserDetailsService userDetailsService;

        public SecurityConfig(JpaUserDetailsService userDetailsService) {
                this.userDetailsService = userDetailsService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // Public resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                                                .permitAll()
                                                // Auth pages
                                                .requestMatchers("/login", "/register", "/forgot-password").permitAll()
                                                // Public pages
                                                .requestMatchers("/", "/home", "/homepage", "/index").permitAll()
                                                // Admin only
                                                .requestMatchers("/admin/**").hasRole("Administrator")
                                                // Organization staff
                                                .requestMatchers("/organization/**")
                                                .hasAnyRole("Organization Staff", "Administrator")
                                                // All authenticated users
                                                .requestMatchers("/profile/**").authenticated()
                                                // All other requests need authentication
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .usernameParameter("email")
                                                .passwordParameter("password")
                                                .defaultSuccessUrl("/profile", true)
                                                .failureUrl("/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false))
                                .userDetailsService(userDetailsService);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}
