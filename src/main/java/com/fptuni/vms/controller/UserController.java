package com.fptuni.vms.controller;

import com.fptuni.vms.model.User;
import com.fptuni.vms.service.RoleService;
import com.fptuni.vms.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

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
            @RequestParam(value = "viewUser", required = false) Integer viewUserId,  // 👈 thêm dòng này

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

        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/userManagement";
    }

    // Tạo mới user
    @PostMapping("/create")
    public String createUser(@ModelAttribute User user,
                             @RequestParam("avatarUrl") String avatarUrl,
                             @RequestParam("citySelect") String city,
                             @RequestParam("districtSelect") String district,
                             @RequestParam("wardSelect") String ward,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        if (userService.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại");
            return "redirect:/admin/users";
        }

        if (userService.existsByPhone(user.getPhone())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã tồn tại");
            return "redirect:/admin/users";
        }
        // Gán ảnh và địa chỉ
        user.setAvatarUrl(
                (avatarUrl == null || avatarUrl.isBlank())
                        ? "/images/default.png"  // ảnh mặc định
                        : avatarUrl
        );
        user.setAddress(String.join(" - ", ward, district, city));

        // Lưu user
        userService.saveUser(user);

        // Thêm flash attribute để show thông báo sau khi redirect
        redirectAttributes.addFlashAttribute("successMessage", "Thêm người dùng thành công!");

        userService.saveUser(user);
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

    // Xóa user
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Người dùng đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}/detail")
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

    @GetMapping("/admin/users/{id}/json")
    @ResponseBody
    public User getUserJson(@PathVariable Integer id) {
        return userService.getUserById(id);
    }



}
