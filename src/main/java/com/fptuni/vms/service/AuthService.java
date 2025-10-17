package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

public interface AuthService {

    class AuthException extends RuntimeException {
        public AuthException(String msg) { super(msg); }
    }

    User login(String email, String rawPassword) throws AuthException;

    // NEW
    User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException;
}
