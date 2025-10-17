// src/main/java/com/fptuni/vms/controller/ErrorController.java
package com.fptuni.vms.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {

    @GetMapping("/403")
    public String forbidden(Model model, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }
}
