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
        if (v == null) throw new IllegalStateException("NOT_LOGGED_IN");
        return (int) v;
    }

    // ---------- LIST ----------
    @GetMapping("/opps-list")
    public String listMyOpps(
            HttpServletRequest req,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            Model model
    ) {
        int ownerId = currentOwnerId(req);
        var p = oppService.listMyOpps(ownerId, page, size, q, categoryId, status);

        model.addAttribute("page", p);
        model.addAttribute("items", p.items());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("categories", categoryService.listAll());

        // view đồng bộ folder "opportunity"
        return "opportunity/opp-list";
    }

    // ---------- CREATE FORM ----------
    @GetMapping("/new")
    public String createForm(HttpServletRequest req, Model model) {
        currentOwnerId(req);
        model.addAttribute("opp", new Opportunity());
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("mode", "CREATE");
        return "opportunity/opp-form"; // <— ĐỔI: đồng bộ folder
    }

    // ---------- EDIT FORM ----------
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") int id, HttpServletRequest req, Model model) {
        int ownerId = currentOwnerId(req);
        Opportunity opp = oppService.getMyOppById(ownerId, id)
                .orElseThrow(() -> new IllegalArgumentException("OPP_NOT_FOUND_OR_NOT_OWNED"));

        model.addAttribute("opp", opp);
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("mode", "EDIT");
        return "opportunity/opp-form"; // <— ĐỔI: đồng bộ folder
    }

    // ---------- CREATE ----------
    @PostMapping("")
    public String create(
            HttpServletRequest req,
            @RequestParam Integer categoryId,
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam Integer neededVolunteers,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Model model
    ) {
        int ownerId = currentOwnerId(req);

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            model.addAttribute("error", "End time must be after start time.");
            model.addAttribute("opp", bindToOpp(null, categoryId, title, subtitle, location, thumbnailUrl, neededVolunteers, startTime, endTime));
            model.addAttribute("categories", categoryService.listAll());
            model.addAttribute("mode", "CREATE");
            return "opportunity/opp-form";
        }

        var opp = bindToOpp(null, categoryId, title, subtitle, location, thumbnailUrl, neededVolunteers, startTime, endTime);
        oppService.createMyOpp(ownerId, opp);
        // Redirect về trang list ĐÚNG URL
        return "redirect:/opportunity/opps-list?msg=created";
    }

    // ---------- UPDATE ----------
    @PostMapping("/{id}")
    public String update(
            @PathVariable("id") int id,
            HttpServletRequest req,
            @RequestParam Integer categoryId,
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam Integer neededVolunteers,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String status,
            Model model
    ) {
        int ownerId = currentOwnerId(req);

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            model.addAttribute("error", "End time must be after start time.");
            Opportunity back = bindToOpp(id, categoryId, title, subtitle, location, thumbnailUrl, neededVolunteers, startTime, endTime);
            if (status != null && !status.isBlank()) {
                try { back.setStatus(Opportunity.OpportunityStatus.valueOf(status)); } catch (Exception ignored) {}
            }
            model.addAttribute("opp", back);
            model.addAttribute("categories", categoryService.listAll());
            model.addAttribute("mode", "EDIT");
            return "opportunity/opp-form";
        }

        Opportunity opp = bindToOpp(id, categoryId, title, subtitle, location, thumbnailUrl, neededVolunteers, startTime, endTime);
        if (status != null && !status.isBlank()) {
            opp.setStatus(Opportunity.OpportunityStatus.valueOf(status));
        }
        oppService.updateMyOpp(ownerId, opp);
        return "redirect:/opportunity/opps-list?msg=updated";
    }

    // ---------- DELETE ----------
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int id, HttpServletRequest req) {
        int ownerId = currentOwnerId(req);
        oppService.deleteMyOpp(ownerId, id);
        return "redirect:/opportunity/opps-list?msg=deleted";
    }

    // ---------- helper ----------
    private Opportunity bindToOpp(Integer id,
                                  Integer categoryId,
                                  String title,
                                  String subtitle,
                                  String location,
                                  String thumbnailUrl,
                                  Integer neededVolunteers,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime) {
        Opportunity o = new Opportunity();
        o.setOppId(id);

        Category c = new Category();
        c.setCategoryId(Objects.requireNonNull(categoryId, "categoryId"));
        o.setCategory(c);

        o.setTitle(title);
        o.setSubtitle(subtitle);
        o.setLocation(location);
        o.setThumbnailUrl(thumbnailUrl);
        o.setNeededVolunteers(neededVolunteers);
        o.setStartTime(startTime);
        o.setEndTime(endTime);
        return o;
    }
}
