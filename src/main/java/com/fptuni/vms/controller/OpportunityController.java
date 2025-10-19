// src/main/java/com/fptuni/vms/controller/OpportunityController.java
package com.fptuni.vms.controller;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.service.CategoryService;
import com.fptuni.vms.service.OpportunityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Controller
@RequestMapping("/opportunity") // <— ĐỔI: dùng /opportunity làm base
public class OpportunityController {

    private final OpportunityService oppService;
    private final CategoryService categoryService;

    public OpportunityController(OpportunityService oppService, CategoryService categoryService) {
        this.oppService = oppService;
        this.categoryService = categoryService;
    }

    private int currentOwnerId(HttpServletRequest req) {
        var ss = req.getSession(false);
        Object v = (ss != null) ? ss.getAttribute("AUTH_USER_ID") : null;
        if (v == null)
            throw new IllegalStateException("NOT_LOGGED_IN");
        return (int) v;
    }

}
