package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.User.UserStatus;
import com.fptuni.vms.repository.RoleRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    public AuthServiceImpl(UserRepository userRepo,
                           RoleRepository roleRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) throws AuthException {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthException("USERNAME_PASSWORD_REQUIRED");
        }

        String normalized = email.trim().toLowerCase();

        User u = userRepo.findByEmail(normalized)
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS"));

        if (u.getStatus() == UserStatus.LOCKED) {
            throw new AuthException("ACCOUNT_LOCKED");
        }

        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS");
        }

        return u;
    }

    @Override
    @Transactional
    public User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException {
        // 1) Kiểm tra trùng email
        Optional<User> existing = userRepo.findByEmail(email != null ? email.trim().toLowerCase() : null);
        if (existing.isPresent()) throw new AuthException("EMAIL_EXISTS");

        // 2) Lấy role VOLUNTEER
        Role vol = roleRepo.findByRoleName("VOLUNTEER")
                .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND"));

        // 3) Hash mật khẩu
        String hash = encoder.encode(rawPassword);

        // 4) Tạo user
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);              // normalize() trong @PrePersist sẽ lower-case
        u.setPhone(phone);
        u.setPasswordHash(hash);
        u.setRole(vol);
        u.setStatus(UserStatus.ACTIVE); // <-- dùng enum, không phải String

        // 5) Lưu
        return userRepo.save(u);
    }
}
