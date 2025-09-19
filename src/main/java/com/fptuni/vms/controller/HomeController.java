package com.fptuni.vms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
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
