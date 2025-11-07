package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.response.ChangePasswordForm;
import com.fptuni.vms.dto.response.ProfileForm;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.CloudinaryService;
import com.fptuni.vms.service.UserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;
    private final RoleServiceImpl roleService;

    public UserServiceImpl(UserRepository userRepository,
                           CloudinaryService cloudinaryService,
                           PasswordEncoder passwordEncoder, RoleServiceImpl roleService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
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


@Override
    public ResponseEntity<InputStreamResource> downloadFileFromUrl(String fileUrl) {
        try {
            // Mở kết nối tới file từ URL
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

            // Lấy tên file từ URL (nếu không có thì đặt mặc định)
            String fileName = extractFileName(fileUrl);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            throw new RuntimeException("Không thể tải file từ URL: " + fileUrl, e);
        }
    }

    @Override
    public int getVolunteerRoleId() {
        return userRepository.findRoleIdByName("VOLUNTEER");
    }



    @Override
    public void generateVolunteerTemplate(OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Volunteer_Template");

        // Tạo style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Hàng 0: Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Họ tên", "Email", "Mật khẩu", "Số điện thoại", "Địa chỉ"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Hàng ví dụ
        String[][] sampleData = {
                {"Nguyễn Văn A", "vana@example.com", "Matkhau123", "0909123456", "Phường 1 - Quận 1 - TP.HCM"},
                {"Trần Thị B", "thib@example.com", "Pass4567", "0912345678", "Phường Ninh Kiều - TP Cần Thơ"},
                {"Lê Văn C", "vanc@example.com", "Volunteer1", "0923456789", "Phường Bến Nghé - Quận 1 - TP.HCM"},
                {"Phạm Thị D", "thid@example.com", "Password9", "0934567890", "Phường Thảo Điền - TP Thủ Đức - TP.HCM"}
        };

        for (int i = 0; i < sampleData.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < sampleData[i].length; j++) {
                row.createCell(j).setCellValue(sampleData[i][j]);
            }
        }

        // Tự động chỉnh độ rộng cột
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);
        workbook.close();
    }


    @Override
    public List<User> parseVolunteerExcel(MultipartFile file, Map<Integer, List<String>> errorMap) {
        List<User> volunteers = new ArrayList<>();
        Set<String> emailSet = new HashSet<>();
        Set<String> phoneSet = new HashSet<>();

        Role volunteerRole = roleService.getRoleByName("VOLUNTEER");

        if (volunteerRole == null) {
            throw new RuntimeException("Role 'VOLUNTEER' không tồn tại.");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            Pattern passwordPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
            Pattern phonePattern = Pattern.compile("^0\\d{9,10}$");
            Pattern namePattern = Pattern.compile("^[\\p{L} .'-]+$");
            Pattern addressPattern = Pattern.compile("^[\\p{L}0-9 ,./-]+$"); // Cho phép chữ, số, dấu phẩy, dấu chấm, gạch

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                boolean isEmptyRow = true;
                for (int c = 0; c <= 4; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        String value = getStringCell(cell).replaceAll("\\s+", "");
                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                if (isEmptyRow) continue;

                String fullName = getStringCell(row.getCell(0));
                String email = getStringCell(row.getCell(1)).toLowerCase();
                String password = getStringCell(row.getCell(2));
                String phone = getStringCell(row.getCell(3));
                String address = getStringCell(row.getCell(4));

                List<String> errors = new ArrayList<>();

                // Validate họ tên
                if (fullName.isBlank()) {
                    errors.add("Tên không được để trống");
                } else if (!namePattern.matcher(fullName).matches()) {
                    errors.add("Tên chứa ký tự không hợp lệ");
                }

                // Validate email
                if (!emailPattern.matcher(email).matches()) {
                    errors.add("Email không hợp lệ");
                } else {
                    if (!emailSet.add(email)) {
                        errors.add("Email trùng lặp trong file Excel");
                    }
                    if (userRepository.existsByEmail(email)) {
                        errors.add("Email đã tồn tại trong hệ thống");
                    }
                }

                // Validate phone
                if (!phonePattern.matcher(phone).matches()) {
                    errors.add("Số điện thoại không hợp lệ");
                } else {
                    if (!phoneSet.add(phone)) {
                        errors.add("Số điện thoại trùng lặp trong file Excel");
                    }
                }

                // Validate password
                if (!passwordPattern.matcher(password).matches()) {
                    errors.add("Mật khẩu phải ≥ 8 ký tự và gồm cả số và chữ");
                }

                // Validate địa chỉ
                if (address.isBlank()) {
                    errors.add("Địa chỉ không được để trống");
                } else if (!addressPattern.matcher(address).matches()) {
                    errors.add("Địa chỉ chứa ký tự không hợp lệ");
                }

                if (!errors.isEmpty()) {
                    errorMap.put(i, errors);

                    // vẫn tạo user để giữ đúng thứ tự dòng khi render
                    User invalidUser = new User();
                    invalidUser.setFullName(fullName);
                    invalidUser.setEmail(email);
                    invalidUser.setPasswordHash(password);
                    invalidUser.setPhone(phone);
                    invalidUser.setAddress(address);
                    invalidUser.setRole(volunteerRole);
                    invalidUser.setStatus(User.UserStatus.LOCKED); // đánh dấu dòng lỗi
                    volunteers.add(invalidUser);

                    continue; // giữ continue, nhưng đã add vào list trước đó
                }


                // Tạo user hợp lệ
                User u = new User();
                u.setFullName(fullName);
                u.setEmail(email);
                u.setPasswordHash(password); // mã hóa tại bước lưu DB
                u.setPhone(phone);
                u.setAddress(address);
                u.setRole(volunteerRole);
                u.setStatus(User.UserStatus.ACTIVE);

                volunteers.add(u);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc Excel: " + e.getMessage());
        }
        return volunteers;
    }


    private String getStringCell(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }


    @Override
    public boolean saveVolunteerList(List<User> volunteers) {
        try {
            Role volunteerRole = roleService.getRoleByName("VOLUNTEER");
            if (volunteerRole == null) {
                throw new RuntimeException("Role 'VOLUNTEER' chưa được cấu hình trong Database");
            }

            for (User u : volunteers) {
                u.setRole(volunteerRole); // Gán role
                u.setStatus(User.UserStatus.ACTIVE); // Trạng thái mặc định
                if (u.getPasswordHash() == null) {
                    // nếu file excel chưa có mật khẩu, generate ngẫu nhiên
                    u.setPasswordHash(passwordEncoder.encode("123456"));
                }
                userRepository.insertVolunteer(u); // hoặc save(user);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    private String extractFileName(String fileUrl) {
        try {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return "downloaded_file";
        }
    }
}
