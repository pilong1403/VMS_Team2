// com/fptuni/vms/security/AuthzService.java
package com.fptuni.vms.security;

import com.fptuni.vms.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthzService {
    public boolean hasRole(User u, String role) {
        return u != null && u.getRole() != null
                && role.equalsIgnoreCase(u.getRole().getRoleName());
    }
}
