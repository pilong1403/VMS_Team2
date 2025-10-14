package com.fptuni.vms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fptuni.vms.model.User;

public class SecurityUtils {

    /**
     * Get current authenticated user from Security Context
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        }

        return null;
    }

    /**
     * Get current authenticated user from Authentication object
     */
    public static User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        }

        return null;
    }

    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(String roleName) {
        User currentUser = getCurrentUser();
        return currentUser != null &&
                currentUser.getRole() != null &&
                roleName.equals(currentUser.getRole().getRoleName());
    }

    /**
     * Check if current user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }
}