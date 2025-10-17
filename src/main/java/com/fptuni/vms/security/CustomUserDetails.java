// src/main/java/com/fptuni/vms/security/CustomUserDetails.java
package com.fptuni.vms.security;

import com.fptuni.vms.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final User user;
    public CustomUserDetails(User u) { this.user = u; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // KHỚP CHÍNH XÁC SecurityConfig.hasAuthority("ORG_OWNER")
        String roleName = user.getRole().getRoleName(); // "ADMIN" | "ORG_OWNER" | "VOLUNTEER"
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !"LOCKED".equalsIgnoreCase(user.getStatus().name()); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public User getDomainUser() { return user; }
}
