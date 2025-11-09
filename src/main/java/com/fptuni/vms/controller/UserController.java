package com.fptuni.vms.controller;

import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.UploadVolunteerRow;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.RoleService;
import com.fptuni.vms.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CloudStorageService cloudStorageService;

    // Trang danh sách user

    @GetMapping
    public String listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @RequestParam(value = "status", required = false) User.UserStatus status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortField", defaultValue = "createdAt") String sortField,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir,
            @RequestParam(value = "viewUser", required = false) Integer viewUserId,

            Model model) {

        List<User> users = userService.searchUsers(
                keyword, roleId, status, fromDate, toDate, page, size, sortField, sortDir);

        long totalUsers = userService.countAllUsers();
        long filteredUsers = userService.countFilteredUsers(keyword, roleId, status, fromDate, toDate);

        // Tạo list số trang động
        List<Integer> pageSizes = new ArrayList<>();
        pageSizes.add(5); // mặc định nhỏ nhất
        if (totalUsers > 10) pageSizes.add((int)Math.min(totalUsers, Math.round(totalUsers * 0.1))); // ~10%
        if (totalUsers > 20) pageSizes.add((int)Math.min(totalUsers, Math.round(totalUsers * 0.4))); // ~40%
        if (totalUsers > 50) pageSizes.add((int)Math.min(totalUsers, Math.round(totalUsers * 0.8))); // ~80%
        int toltal= (int) totalUsers;
        pageSizes.add(toltal); // luôn thêm 100%

        // Loại trùng và sắp xếp tăng dần
        pageSizes = pageSizes.stream().distinct().sorted().toList();

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("pageSizes", pageSizes);
        model.addAttribute("filteredUsers", filteredUsers);

        if (viewUserId != null) {
            User selectedUser = userService.getUserById(viewUserId);
            model.addAttribute("selectedUser", selectedUser);
            model.addAttribute("showUserDetailModal", true);
        }

        // giữ state cho view
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("sortField", sortField);

        model.addAttribute("activePage", "users");
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/userManagement";
    }

    @PostMapping("/create")
    public String createUser(
            @ModelAttribute User user,
            @RequestParam("avatarFile") MultipartFile avatarFile,
            @RequestParam("citySelect") String city,
            @RequestParam("districtSelect") String district,
            @RequestParam("wardSelect") String ward,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra email & phone
        if (userService.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại");
            return "redirect:/admin/users";
        }
        if (userService.existsByPhone(user.getPhone())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã tồn tại");
            return "redirect:/admin/users";
        }

        // Xử lý upload ảnh
        String avatarUrl;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarUrl = cloudStorageService.uploadFile(avatarFile);
            if (avatarUrl == null) {
                avatarUrl = "https://res.cloudinary.com/vmscloudinary/image/upload/v1760954395/User_Avt_Default_chq9k6.jpg";
            }
        } else {
            avatarUrl = "https://res.cloudinary.com/vmscloudinary/image/upload/v1760954395/User_Avt_Default_chq9k6.jpg";
        }
        user.setAvatarUrl(avatarUrl);

        // Gán địa chỉ
        user.setAddress(String.join(" - ", ward, district, city));

        // Lưu user
        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm người dùng thành công!");
        return "redirect:/admin/users";
    }


    // Khóa / Mở khóa user
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            User u = userService.getUserById(id);
            if (u == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng có ID: " + id);
                return "redirect:/admin/users";
            }

            User.UserStatus current = u.getStatus();
            User.UserStatus next = (current == User.UserStatus.ACTIVE)
                    ? User.UserStatus.LOCKED
                    : User.UserStatus.ACTIVE;

            u.setStatus(next);
            userService.saveUser(u);

            String msg = (next == User.UserStatus.ACTIVE)
                    ? "Người dùng đã được mở khóa thành công!"
                    : "Người dùng đã bị khóa!";
            redirectAttributes.addFlashAttribute("successMessage", msg);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }



    @GetMapping("/{id}/detail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable("id") Integer id) {
        User u = userService.getUserById(id);
        if (u == null) return ResponseEntity.notFound().build();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", u.getUserId());
        data.put("fullName", u.getFullName());
        data.put("email", u.getEmail());
        data.put("phone", u.getPhone());
        data.put("roleName", u.getRole().getRoleName());
        data.put("status", u.getStatus().toString());
        data.put("avatarUrl", u.getAvatarUrl());
        data.put("address", u.getAddress());
        data.put("createdAt", u.getCreatedAt().toString());

        return ResponseEntity.ok(data);
    }



    @GetMapping("/export/excel/{id}")
    public void exportUserExcel(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        User user = userService.getUserById(id);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy người dùng!");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + id + ".xlsx");

        userService.exportUserToExcel(user, response.getOutputStream());
    }

    @GetMapping("/{id}/json")
    @ResponseBody
    public User getUserJson(@PathVariable Integer id) {
        return userService.getUserById(id);
    }


    @GetMapping("/download-file")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("url") String fileUrl) {
        return userService.downloadFileFromUrl(fileUrl);
    }

    @PostMapping("/upload-volunteer-excel")
    public String uploadVolunteerExcel(@RequestParam("excelFile") MultipartFile file,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {

        // clear session cũ
        session.removeAttribute("volunteerListSession");
        session.removeAttribute("uploadRows");
        session.removeAttribute("totalCount");
        session.removeAttribute("errorCount");
        session.removeAttribute("validCount");

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file Excel!");
            return "redirect:/admin/users";
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                !(filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ cho phép file Excel (.xlsx hoặc .xls).");
            return "redirect:/admin/users";
        }

        Map<Integer, List<String>> errorMap = new HashMap<>();
        List<User> volunteerList = userService.parseVolunteerExcel(file, errorMap);

        if (volunteerList.size() > 100) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mỗi lần chỉ được upload tối đa 100 volunteer. File hiện có " + volunteerList.size() + " dòng.");
            return "redirect:/admin/users";
        }

        int totalCount = volunteerList.size();
        int errorCount = errorMap.size();
        int validCount = totalCount - errorCount;

        // build danh sách row hiển thị
        List<UploadVolunteerRow> rows = new ArrayList<>();
        for (int i = 0; i < volunteerList.size(); i++) {
            int rowIndex = i + 1;
            User u = volunteerList.get(i);
            List<String> errs = errorMap.getOrDefault(rowIndex, List.of());

            UploadVolunteerRow row = new UploadVolunteerRow(rowIndex, u, errs);
            rows.add(row);
        }

        // lưu vào session cho trang check-upload dùng
        session.setAttribute("volunteerListSession", volunteerList); // confirm-volunteer cần
        session.setAttribute("uploadRows", rows);
        session.setAttribute("totalCount", totalCount);
        session.setAttribute("errorCount", errorCount);
        session.setAttribute("validCount", validCount);

        // chuyển sang GET để hỗ trợ phân trang / lọc
        return "redirect:/admin/users/check-upload";
    }



    @GetMapping("/check-upload")
    public String checkUploadPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status, // all | valid | error
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {

        @SuppressWarnings("unchecked")
        List<UploadVolunteerRow> all =
                (List<UploadVolunteerRow>) session.getAttribute("uploadRows");

        if (all == null) {
            // F5 thẳng trang này mà chưa upload file
            return "redirect:/admin/users";
        }

        Integer totalCount = (Integer) session.getAttribute("totalCount");
        Integer errorCount = (Integer) session.getAttribute("errorCount");
        Integer validCount = (Integer) session.getAttribute("validCount");

        if (keyword == null || "null".equalsIgnoreCase(keyword)) {
            keyword = "";
        }
        String kw = keyword.trim().toLowerCase();

        // lọc theo trạng thái + search tên/sđt
        List<UploadVolunteerRow> filtered = all.stream()
                .filter(r -> {
                    boolean okStatus =
                            "all".equals(status) ||
                                    ("valid".equals(status) && r.isValid()) ||
                                    ("error".equals(status) && !r.isValid());

                    String name  = Optional.ofNullable(r.getUser().getFullName()).orElse("").toLowerCase();
                    String phone = Optional.ofNullable(r.getUser().getPhone()).orElse("").toLowerCase();

                    boolean okSearch = kw.isEmpty()
                            || name.contains(kw)
                            || phone.contains(kw);

                    return okStatus && okSearch;
                })
                .toList();

        int total = filtered.size();
        int totalPages = (int) Math.ceil(total / (double) size);
        if (totalPages == 0) totalPages = 1;

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int from = page * size;
        int to = Math.min(from + size, total);
        List<UploadVolunteerRow> pageRows = filtered.subList(from, to);

        model.addAttribute("userList", pageRows);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("errorCount", errorCount);
        model.addAttribute("validCount", validCount);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "admin/check-upload-file-user";
    }




    @PostMapping("/confirm-volunteer-excel")
    public String confirmVolunteerExcel(HttpSession session, RedirectAttributes redirectAttributes) {
        List<User> volunteers = (List<User>) session.getAttribute("volunteerListSession");

        if (volunteers == null || volunteers.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có dữ liệu volunteer để lưu!");
            return "redirect:/admin/users";
        }

        boolean success = userService.saveVolunteerList(volunteers);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm volunteer thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi lưu volunteer!");
        }

        session.removeAttribute("volunteerListSession");
        return "redirect:/admin/users";
    }

    @GetMapping("/bulk/template")
    public void downloadVolunteerTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Volunteer_Template.xlsx");
        userService.generateVolunteerTemplate(response.getOutputStream());
    }



}
