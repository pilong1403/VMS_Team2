// src/main/java/com/fptuni/vms/service/impl/AuthServiceImpl.java
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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    // Email regex đơn giản, đủ dùng ở mức cơ bản
    private static final Pattern EMAIL_RE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
            throw new AuthException(ErrorCode.USERNAME_PASSWORD_REQUIRED);
        }

        String normalized = email.trim().toLowerCase();

        // JOIN FETCH role để khi build CustomUserDetails không bị lazy ở ngoài
        User u = userRepo.findByEmailWithRole(normalized)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

        if (u.getStatus() == UserStatus.LOCKED) {
            throw new AuthException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        return u;
    }

    @Override
    @Transactional
    public User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException {
        // ---- Validate đầu vào cơ bản
        if (fullName == null || fullName.isBlank()
                || email == null || email.isBlank()
                || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthException(ErrorCode.INVALID_INPUT, "Thiếu họ tên, email hoặc mật khẩu.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalizedEmail).matches()) {
            throw new AuthException(ErrorCode.INVALID_EMAIL, "Email không hợp lệ.");
        }

        // Mật khẩu tối thiểu: 8 ký tự (có thể nâng cấp rule sau)
        if (rawPassword.length() < 8) {
            throw new AuthException(ErrorCode.WEAK_PASSWORD, "Mật khẩu quá ngắn (>= 8 ký tự).");
        }

        // ---- Kiểm tra trùng email
        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new AuthException(ErrorCode.EMAIL_EXISTS, "Email đã được sử dụng.");
        }

        // ---- Lấy role VOLUNTEER
        Role vol = roleRepo.findByRoleName("VOLUNTEER")
                .orElseThrow(() -> new AuthException(ErrorCode.SYSTEM_ERROR, "Không tìm thấy role VOLUNTEER."));

        // ---- Hash mật khẩu
        String hash = encoder.encode(rawPassword);

        // ---- Tạo user
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(normalizedEmail); // normalize() trong @PrePersist vẫn hoạt động, nhưng set sạch luôn ở đây
        u.setPhone(phone != null ? phone.trim() : null);
        u.setPasswordHash(hash);
        u.setRole(vol);
        u.setStatus(UserStatus.ACTIVE);

        // ---- Lưu
        return userRepo.save(u);
    }
}
