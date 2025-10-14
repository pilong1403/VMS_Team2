package com.fptuni.vms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Thông tin đăng nhập không chính xác!");
        }

        if (logout != null) {
            model.addAttribute("success", "Đăng xuất thành công!");
        }

        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout() {
        // Spring Security will handle the actual logout
        return "redirect:/login?logout=true";
    }
}
