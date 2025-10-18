package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.ChangePasswordForm;
import com.fptuni.vms.dto.ProfileForm;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.CloudinaryService;
import com.fptuni.vms.service.UserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

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



    // ===== CRUD =====
    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }



    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> searchUsers(String keyword, Integer roleId,
                                  User.UserStatus status,
                                  LocalDate fromDate, LocalDate toDate,
                                  int page, int size,
                                  String sortField, String sortDir) {
        return userRepository.search(keyword, roleId, status, fromDate, toDate, page, size, sortField, sortDir);
    }

    @Override
    public long countFilteredUsers(String keyword, Integer roleId,
                                   User.UserStatus status,
                                   LocalDate fromDate, LocalDate toDate) {
        return userRepository.countFiltered(keyword, roleId, status, fromDate, toDate);
    }


    // ===== STATISTICS =====
    @Override
    public long countAllUsers() {
        return userRepository.countAll();
    }

    @Override
    public long countUsersByStatus(String status) {
        return userRepository.countByStatus(status);
    }

//    @Override
//    public boolean existsByEmail(String email) {
//        return userRepository.existsByEmail(email);    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public void exportUserToExcel(User user, OutputStream os) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Detail");

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Thông tin người dùng");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        sheet.createRow(rowIdx++);

        String[][] data = {
                {"Họ tên", user.getFullName()},
                {"Email", user.getEmail()},
                {"Số điện thoại", user.getPhone() != null ? user.getPhone() : ""},
                {"Vai trò", user.getRole().getRoleName()},
                {"Địa chỉ", user.getAddress() != null ? user.getAddress() : ""},
                {"Trạng thái", user.getStatus().toString()},
                {"Ngày tạo", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""}
        };

        for (String[] rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(rowData[0]);
            row.createCell(1).setCellValue(rowData[1]);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        workbook.write(os);
        workbook.close();
    }

    @Override
    public List<User> getUsersByRole(Integer roleId) {
        return userRepository.getUsersByRole(roleId);
    }

//    @Override
//    public User findByEmail(String email) {
//        return userRepository.findByEmail(email);
//    }


}
