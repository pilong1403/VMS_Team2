package com.fptuni.vms.controller;

import com.fptuni.vms.model.User;
import com.fptuni.vms.service.RoleService;
import com.fptuni.vms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir,
            Model model) {

        List<User> users = userService.searchUsers(keyword, roleId, page, size, sortDir);
        long totalUsers = userService.countAllUsers();
        long filteredUsers = userService.countFilteredUsers(keyword, roleId);

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("filteredUsers", filteredUsers);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("roles", roleService.getAllRoles()); // thêm dòng này

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

        // Check trùng email
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("errorMessage", "Email đã tồn tại");
            model.addAttribute("roles", roleService.getAllRoles());
            model.addAttribute("users", userService.getAllUsers());
            return "admin/userManagement";
        }

        // Check trùng phone
        if (userService.existsByPhone(user.getPhone())) {
            model.addAttribute("errorMessage", "Số điện thoại đã tồn tại");
            model.addAttribute("roles", roleService.getAllRoles());
            model.addAttribute("users", userService.getAllUsers());
            return "admin/userManagement";
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
    public String toggleStatus(@PathVariable("id") Integer id) {
        User u = userService.getUserById(id);
        if (u != null) {
            User.UserStatus current = u.getStatus();
            User.UserStatus next = (current == User.UserStatus.ACTIVE) ? User.UserStatus.LOCKED : User.UserStatus.ACTIVE;
            u.setStatus(next);
            userService.saveUser(u);
        }
        return "redirect:/admin/users";
    }

    // Xóa user
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable("id") Integer id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
