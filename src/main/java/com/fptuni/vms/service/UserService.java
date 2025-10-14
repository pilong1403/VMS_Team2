package com.fptuni.vms.service;

import com.fptuni.vms.dto.ChangePasswordForm;
import com.fptuni.vms.dto.ProfileForm;
import com.fptuni.vms.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    void updateProfile(Integer userId, ProfileForm profileForm, MultipartFile avatarFile) throws Exception;

    void changePassword(Integer userId, ChangePasswordForm changePasswordForm) throws Exception;

    User findById(Integer userId);

    User findByIdWithRole(Integer userId);

    User findByEmail(String email);

    User save(User user);
}
