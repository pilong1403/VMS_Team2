package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OrgAccountRegisterForm;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/auth")
public class OrgRegisterController {

    private final UserService userService;
    private final OrganizationService orgService;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    public OrgRegisterController(UserService userService,
                                 OrganizationService orgService,
                                 RoleRepository roleRepo,
                                 PasswordEncoder encoder) {
        this.userService = userService;
        this.orgService = orgService;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
    }

    @GetMapping("/org-register")
    public String showForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new OrgAccountRegisterForm());
        }
        return "auth/org-register";
    }

    @PostMapping("/org-register")
    @Transactional
    public String submit(@Valid @ModelAttribute("form") OrgAccountRegisterForm form,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.form", br);
            ra.addFlashAttribute("form", form);
            return "redirect:/auth/org-register";
        }

        try {
            // 1. Kiểm tra email đã tồn tại
            if (userService.existsByEmail(form.getEmail().trim().toLowerCase())) {
                ra.addFlashAttribute("error", "Email đã được sử dụng.");
                ra.addFlashAttribute("form", form);
                return "redirect:/auth/org-register";
            }

            // 2. Lấy role ORG_OWNER
            Role orgOwnerRole = roleRepo.findByRoleName("ORG_OWNER")
                    .orElseThrow(() -> new IllegalStateException("Thiếu role ORG_OWNER trong DB."));

            // 3. Tạo user (ORG_OWNER)
            User user = new User();
            user.setFullName(form.getFullName());
            user.setEmail(form.getEmail().trim().toLowerCase());
            user.setPasswordHash(encoder.encode(form.getPassword()));
            user.setPhone(form.getPhone());
            user.setRole(orgOwnerRole);
            user.setStatus(User.UserStatus.ACTIVE);
            userService.save(user);

            // 4. Gửi đăng ký tổ chức (PENDING)
            orgService.submitRegistration(
                    user,
                    form.getOrgName(),
                    form.getDescription(),
                    form.getRegDocUrl(),
                    form.getNote()
            );

            ra.addFlashAttribute("msg", "Đăng ký thành công! Hồ sơ của bạn đang chờ phê duyệt.");
            return "redirect:/login";

        } catch (OrganizationService.OrgException e) {
            String message = switch (e.getMessage()) {
                case "OWNER_ROLE_REQUIRED" -> "Tài khoản phải có vai trò ORG_OWNER.";
                case "OWNER_ALREADY_HAS_ORG" -> "Mỗi chủ sở hữu chỉ được đăng ký 01 tổ chức.";
                case "CONSTRAINT_VIOLATION" -> "Vi phạm ràng buộc dữ liệu. Vui lòng kiểm tra lại.";
                default -> "Không thể gửi đăng ký tổ chức. Vui lòng thử lại.";
            };
            ra.addFlashAttribute("error", message);
            ra.addFlashAttribute("form", form);
            return "redirect:/auth/org-register";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/auth/org-register";
        }
    }
}
