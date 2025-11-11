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

        // Tìm user theo email
        User u = userRepo.findByEmailWithRole(normalized)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        // Kiểm tra mật khẩu
        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String roleName = u.getRole().getRoleName();

        // Kiểm tra tài khoản Org Owner
        if ("ORG_OWNER".equals(roleName)) {
            var orgOpt = orgRepo.findByOwnerId(u.getUserId()); // Lấy hồ sơ tổ chức của người dùng
            if (orgOpt.isPresent()) {
                String reg = orgOpt.get().getRegStatus().name(); // Lấy trạng thái hồ sơ tổ chức

                // Kiểm tra trạng thái hồ sơ tổ chức
                if ("PENDING".equalsIgnoreCase(reg)) {
                    throw new AuthException(AuthErrorCode.ORG_PENDING,
                            "Tài khoản của bạn chưa thể đăng nhập vì hồ sơ tổ chức đang chờ duyệt.");
                }

                if ("REJECTED".equalsIgnoreCase(reg)) {
                    throw new AuthException(AuthErrorCode.ORG_REJECTED,
                            "Hồ sơ tổ chức của bạn đã bị từ chối. Vui lòng đăng ký lại.");
                }

                // Hồ sơ tổ chức đã được APPROVED, kiểm tra trạng thái tài khoản
                if ("APPROVED".equalsIgnoreCase(reg)) {
                    // Kiểm tra trạng thái tài khoản
                    if (u.getStatus() == User.UserStatus.LOCKED) {
                        throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED,
                                "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
                    }
                    if (u.getStatus() == User.UserStatus.ACTIVE) {
                        // Tài khoản ACTIVE và hồ sơ tổ chức APPROVED => Cho phép đăng nhập
                        return u;
                    }
                }
            } else {
                // Nếu không tìm thấy hồ sơ tổ chức, thông báo lỗi
                throw new AuthException(AuthErrorCode.ORG_REJECTED,
                        "Không tìm thấy hồ sơ tổ chức của bạn. Vui lòng liên hệ quản trị viên.");
            }
        } else {
            // Các role khác (không phải Org Owner), kiểm tra tài khoản bị khóa
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
            throw new AuthException(AuthErrorCode.INVALID_INPUT, "Thiếu họ tên, email hoặc mật khẩu.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalizedEmail).matches()) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL, "Email không hợp lệ.");
        }
        if (rawPassword.length() < 8) {
            throw new AuthException(AuthErrorCode.WEAK_PASSWORD, "Mật khẩu quá ngắn (>= 8 ký tự).");
        }

        // Nếu email đã tồn tại, kiểm tra nhánh "đăng ký lại" cho ORG_OWNER có org REJECTED
        var existedOpt = userRepo.findByEmailWithRole(normalizedEmail);
        if (existedOpt.isPresent()) {
            User existed = existedOpt.get();
            String roleName = existed.getRole() != null ? existed.getRole().getRoleName() : null;

            if ("ORG_OWNER".equalsIgnoreCase(roleName)) {
                var orgOpt = orgRepo.findByOwnerId(existed.getUserId());
                if (orgOpt.isPresent()
                        && orgOpt.get().getRegStatus() == com.fptuni.vms.model.Organization.RegStatus.REJECTED) {
                    // Cho phép "đăng ký lại": cập nhật user hiện có để tránh unique email
                    existed.setFullName(fullName.trim());
                    existed.setPhone(phone != null ? phone.trim() : null);
                    existed.setAddress(address != null ? address.trim() : null);
                    existed.setAvatarUrl(avatarUrl);
                    existed.setPasswordHash(encoder.encode(rawPassword));
                    existed.setStatus(UserStatus.LOCKED); // khóa lại chờ duyệt hồ sơ mới
                    return userRepo.save(existed); // merge
                }
            }
            // Các trường hợp còn lại => chặn
            throw new AuthException(AuthErrorCode.EMAIL_EXISTS, "Email đã được sử dụng.");
        }

        // Tạo mới bình thường
        Role ownerRole = roleRepo.findByRoleName("ORG_OWNER")
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR, "Không tìm thấy role ORG_OWNER."));

        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(normalizedEmail);
        u.setPhone(phone != null ? phone.trim() : null);
        u.setAddress(address != null ? address.trim() : null);
        u.setAvatarUrl(avatarUrl);
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

        // Nếu email chưa tồn tại => OK
        if (!userRepo.existsByEmail(normalized)) return;

        // Email đã tồn tại -> xem có thuộc owner bị REJECTED hay không
        var uOpt = userRepo.findByEmailWithRole(normalized);
        if (uOpt.isEmpty()) {
            throw new AuthException(AuthErrorCode.EMAIL_EXISTS, "Email đã tồn tại trong hệ thống.");
        }

        User u = uOpt.get();
        String roleName = (u.getRole() != null ? u.getRole().getRoleName() : null);

        // Chỉ cho phép “đăng ký lại” nếu là ORG_OWNER và hồ sơ org đang REJECTED
        if ("ORG_OWNER".equalsIgnoreCase(roleName)) {
            var orgOpt = orgRepo.findByOwnerId(u.getUserId()); // repo đã có method này
            if (orgOpt.isPresent() && orgOpt.get().getRegStatus() == com.fptuni.vms.model.Organization.RegStatus.REJECTED) {
                // Cho phép dùng lại email để đăng ký (sẽ là nhánh update user)
                return;
            }
        }

        // Các trường hợp còn lại vẫn chặn
        throw new AuthException(AuthErrorCode.EMAIL_EXISTS, "Email đã tồn tại trong hệ thống. Vui lòng sử dụng tài khoản khác.");
    }

}
