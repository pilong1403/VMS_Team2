// com.fptuni.vms.security.JpaUserDetailsService.java
package com.fptuni.vms.security;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public JpaUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));
        return new CustomUserDetails(u);
    }
}
