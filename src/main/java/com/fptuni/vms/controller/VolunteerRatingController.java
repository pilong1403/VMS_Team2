package com.fptuni.vms.controller;

import com.fptuni.vms.service.VolunteerRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/ratings")
public class VolunteerRatingController {

    @Autowired
    private VolunteerRatingService ratingService;

    @GetMapping
    public String listRatings(@RequestParam(value = "stars", required = false) Short stars,
                              Model model) {
        if (stars != null) {
            model.addAttribute("ratings", ratingService.getRatingsByStars(stars));
            model.addAttribute("filteredStars", stars);
        } else {
            model.addAttribute("ratings", ratingService.getAllRatings());
        }
        model.addAttribute("totalRatings", ratingService.countAllRatings());
        return "admin/reportManagement"; // Trang hiển thị báo cáo
    }
}
