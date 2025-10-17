package com.fptuni.vms.service;

import com.fptuni.vms.dto.ChangePasswordForm;
import com.fptuni.vms.dto.ProfileForm;
import com.fptuni.vms.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface UserService {

    /** Lưu/sửa người dùng */
    User save(User user);

    /** Email đã tồn tại chưa (dùng khi đăng ký) */
    boolean existsByEmail(String email);

    /** Tìm theo email (dùng khi đăng nhập) */
    Optional<User> findByEmail(String email);

    /** Tìm theo id (tiện lấy user từ session id) */
    Optional<User> findById(Integer id);

    void updateProfile(Integer userId, ProfileForm profileForm, MultipartFile avatarFile) throws Exception;

    void changePassword(Integer userId, ChangePasswordForm changePasswordForm) throws Exception;

    User findByIdWithRole(Integer userId);

}
