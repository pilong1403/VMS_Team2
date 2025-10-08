package com.fptuni.vms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {



    @GetMapping("/organizations")
    public String organizationPage() {
        return "admin/organizationManagement";
    }

    @GetMapping("/reports")
    public String reportPage() {
        return "admin/reportManagement";
    }
}
