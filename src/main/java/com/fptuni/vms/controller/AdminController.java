package com.fptuni.vms.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class AdminController {
    @GetMapping("/homepage")
    public String homepage() {
        // Trả về file homepage.html trong thư mục templates
        return "homepage";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }
}
