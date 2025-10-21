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


}
