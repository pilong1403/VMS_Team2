package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.ChangePasswordForm;
import com.fptuni.vms.dto.ProfileForm;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.CloudinaryService;
import com.fptuni.vms.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           CloudinaryService cloudinaryService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User user) {
        // Có thể chuẩn hóa dữ liệu trước khi lưu nếu cần
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public void updateProfile(Integer userId, ProfileForm profileForm, MultipartFile avatarFile) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Update basic information
        user.setFullName(profileForm.getFullName());
        user.setEmail(profileForm.getEmail());
        user.setPhone(profileForm.getPhone());
        user.setAddress(profileForm.getAddress());
        user.setUpdatedAt(LocalDateTime.now());

        // Handle avatar upload
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // Delete old avatar if exists
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.trim().isEmpty()) {
                    String oldPublicId = cloudinaryService.extractPublicId(oldAvatarUrl);
                    if (oldPublicId != null) {
                        cloudinaryService.deleteImage(oldPublicId);
                    }
                }

                // Upload new avatar
                String newAvatarUrl = cloudinaryService.uploadImage(avatarFile);
                user.setAvatarUrl(newAvatarUrl);

            } catch (Exception e) {
                throw new Exception("Failed to upload avatar: " + e.getMessage(), e);
            }
        }

        // Save updated user
        userRepository.save(user);
    }

    @Override
    public void changePassword(Integer userId, ChangePasswordForm changePasswordForm) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Validate current password
        if (!passwordEncoder.matches(changePasswordForm.getCurrentPassword(), user.getPasswordHash())) {
            throw new Exception("Mật khẩu hiện tại không đúng");
        }

        // Validate password confirmation
        if (!changePasswordForm.isPasswordsMatch()) {
            throw new Exception("Mật khẩu xác nhận không khớp");
        }

        // Encode and set new password
        String encodedNewPassword = passwordEncoder.encode(changePasswordForm.getNewPassword());
        user.setPasswordHash(encodedNewPassword);
        user.setUpdatedAt(LocalDateTime.now());

        // Save updated user
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByIdWithRole(Integer userId) {
        return userRepository.findByIdWithRole(userId).orElse(null);
    }

}
