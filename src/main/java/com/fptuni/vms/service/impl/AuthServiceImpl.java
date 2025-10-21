package com.fptuni.vms.service.impl;

import com.fptuni.vms.enums.AuthErrorCode;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.User.UserStatus;
import com.fptuni.vms.repository.RoleRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    private static final Pattern EMAIL_RE =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
            throw new AuthException(AuthErrorCode.USERNAME_PASSWORD_REQUIRED);
        }

        String normalized = email.trim().toLowerCase();

        User u = userRepo.findByEmailWithRole(normalized)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        if (u.getStatus() == UserStatus.LOCKED) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return u;
    }

    @Override
    @Transactional
    public User registerVolunteer(String fullName, String email,
                                  String phone, String rawPassword) throws AuthException {

        if (fullName == null || fullName.isBlank()
                || email == null || email.isBlank()
                || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_INPUT,
                    "Thiếu họ tên, email hoặc mật khẩu.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalizedEmail).matches()) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL, "Email không hợp lệ.");
        }

        if (rawPassword.length() < 8) {
            throw new AuthException(AuthErrorCode.WEAK_PASSWORD, "Mật khẩu quá ngắn (>= 8 ký tự).");
        }

        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new AuthException(AuthErrorCode.EMAIL_EXISTS, "Email đã được sử dụng.");
        }

        Role vol = roleRepo.findByRoleName("VOLUNTEER")
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR,
                        "Không tìm thấy role VOLUNTEER."));

        User u = new User();
        u.setFullName(fullName);
        u.setEmail(normalizedEmail);
        u.setPhone(phone != null ? phone.trim() : null);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(vol);
        u.setStatus(UserStatus.ACTIVE);

        return userRepo.save(u);
    }
}
