// src/main/java/com/fptuni/vms/service/impl/AuthServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.enums.AuthErrorCode;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.User.UserStatus;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.repository.RoleRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_RE =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final OrganizationRepository orgRepo; // NEW
    private final PasswordEncoder encoder;

    public AuthServiceImpl(UserRepository userRepo,
                           RoleRepository roleRepo,
                           OrganizationRepository orgRepo,  // NEW
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.orgRepo  = orgRepo;   // NEW
        this.encoder  = encoder;
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

        // Kiểm tra mật khẩu trước
        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String roleName = u.getRole().getRoleName();

        if ("ORG_OWNER".equals(roleName)) {
            var orgOpt = orgRepo.findByOwnerId(u.getUserId()); // dùng đúng method của bạn
            if (u.getStatus() == User.UserStatus.LOCKED && orgOpt.isPresent()) {
                String reg = orgOpt.get().getRegStatus().name(); // hoặc getRegStatus().toString()
                if ("PENDING".equalsIgnoreCase(reg)) {
                    throw new AuthException(AuthErrorCode.ORG_PENDING,
                            "Hồ sơ tổ chức đang chờ duyệt. Vui lòng đợi quản trị viên xét duyệt.");
                }
                if ("REJECTED".equalsIgnoreCase(reg)) {
                    throw new AuthException(AuthErrorCode.ORG_REJECTED,
                            "Hồ sơ tổ chức của bạn đã bị từ chối. Vui lòng đăng ký lại.");
                }
                if ("APPROVED".equalsIgnoreCase(reg)) {
                    throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED,
                            "Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên để được hỗ trợ.");
                }
            }
        } else {
            // các role khác
            if (u.getStatus() == User.UserStatus.LOCKED) {
                throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
            }
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
        u.setFullName(fullName.trim());
        u.setEmail(normalizedEmail);
        u.setPhone(phone != null ? phone.trim() : null);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(vol);
        u.setStatus(UserStatus.ACTIVE);

        return userRepo.save(u);
    }

    @Override
    @Transactional
    public User registerOwnerAccount(String fullName,
                                     String email,
                                     String phone,
                                     String rawPassword,
                                     String address,
                                     String avatarUrl) throws AuthException {
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

        Role ownerRole = roleRepo.findByRoleName("ORG_OWNER")
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR,
                        "Không tìm thấy role ORG_OWNER."));

        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(normalizedEmail);
        u.setPhone(phone != null ? phone.trim() : null);
        u.setAddress(address != null ? address.trim() : null); // lưu vào users.address (đúng DB)
        u.setAvatarUrl(avatarUrl);                              // nếu bạn truyền vào
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(ownerRole);
        u.setStatus(UserStatus.LOCKED);

        return userRepo.save(u);
    }
    @Override
    @Transactional(readOnly = true)
    public void assertNewAccountEmailUsable(String email) throws AuthException {
        if (email == null || email.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL, "Vui lòng nhập email.");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalized).matches()) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL, "Email không hợp lệ.");
        }
        if (userRepo.existsByEmail(normalized)) {
            throw new AuthException(
                    AuthErrorCode.EMAIL_EXISTS,
                    "Email đã tồn tại trong hệ thống. Vui lòng sử dụng tài khoản khác."
            );
        }
    }
}
