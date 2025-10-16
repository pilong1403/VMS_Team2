package com.fptuni.vms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        // Trả về trang chủ mới
        return "home/home";
    }

    @GetMapping("/home")
    public String homeAlternative(Model model) {
        // Route thay thế cho trang chủ
        return "home/home";
    }

    @GetMapping("/homepage")
    public String homepage() {
        // Trả về file homepage.html trong thư mục templates (giữ nguyên cho
        // compatibility)
        return "homepage";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    // Các routes placeholder cho các trang được link trong home
    @GetMapping("/about")
    public String about(Model model) {
        // TODO: Implement about page
        model.addAttribute("message", "Trang Giới Thiệu - Coming Soon");
        return "home/home"; // Tạm thời redirect về home
    }

    @GetMapping("/opportunities")
    public String opportunities(Model model) {
        // TODO: Load opportunities from database
        return "home/opportunities";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        // TODO: Implement FAQ page
        model.addAttribute("message", "Trang FAQ - Coming Soon");
        return "home/home"; // Tạm thời redirect về home
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        // TODO: Implement contact page
        model.addAttribute("message", "Trang Liên Hệ - Coming Soon");
        return "home/home"; // Tạm thời redirect về home
    }
}
