package com.fptuni.vms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    /** Trang placeholder đúng với redirect sau đăng nhập ADMIN */
    @GetMapping("/file-giu-cho")
    public String adminPlaceholder() {
        return "admin/file-giu-cho"; // templates/admin/file-giu-cho.html
    }
}
