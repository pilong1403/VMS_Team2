package com.fptuni.vms.controller;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.service.OpportunityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final OpportunityService opportunityService;

    public HomeController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping("/home")
    public String volunteerHome(Model model) {
        try {
            List<OpportunityCardDto> latestOpportunities = opportunityService.getTop3LatestOpportunities();
            model.addAttribute("latestOpportunities", latestOpportunities);
        } catch (Exception e) {
            model.addAttribute("latestOpportunities", List.of());
        }
        return "home/home"; // templates/home/home.html
    }

    @GetMapping("/")
    public String publicHome(Model model) {
        return volunteerHome(model);
    }

    @GetMapping("/home/opportunities")
    public String orgOwnerLanding(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String time,
            Model model) {

        return opportunities(page, size, categoryId, location, status, search, time, model);
    }

    @GetMapping("/opportunities")
    public String opportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String time,
            Model model) {

        try {
            Pageable pageable = PageRequest.of(page, size);

            Page<OpportunityCardDto> opportunityPage;
            if (categoryId != null || location != null || status != null || search != null || time != null) {
                opportunityPage = opportunityService.getOpportunityCardsWithFilters(
                        categoryId, location, status, search, time, "newest", pageable);
            } else {
                opportunityPage = opportunityService.getOpportunityCards(pageable);
            }

            List<Category> categories = opportunityService.getCategoriesWithOpportunities();

            model.addAttribute("opportunities", opportunityPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", opportunityPage.getTotalPages());
            model.addAttribute("totalElements", opportunityPage.getTotalElements());
            model.addAttribute("hasNext", opportunityPage.hasNext());
            model.addAttribute("hasPrevious", opportunityPage.hasPrevious());

            model.addAttribute("categories", categories);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedLocation", location);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("selectedTime", time);

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("opportunities", List.of());
            model.addAttribute("categories", List.of());
            model.addAttribute("totalElements", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
        }

        return "home/opportunities"; // templates/home/opportunities.html
    }

    @GetMapping("/homepage")
    public String homepage(Model model) {
        return volunteerHome(model);
    }

    @GetMapping("/index")
    public String index(Model model) {
        return volunteerHome(model);
    }

    @GetMapping("/about")
    public String about(Model model) {
        // Add any additional data needed for the about page
        model.addAttribute("pageTitle", "Về Chúng Tôi");
        return "public/about"; // templates/public/about.html
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("message", "Trang FAQ - Coming Soon");
        return "home/home";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("message", "Trang Liên Hệ - Coming Soon");
        return "home/home";
    }
}
