package com.fptuni.vms.controller;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.service.OpportunityService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/")
    public String home(Model model) {
        try {
            // Lấy top 3 cơ hội mới nhất
            List<OpportunityCardDto> latestOpportunities = opportunityService.getTop3LatestOpportunities();
            model.addAttribute("latestOpportunities", latestOpportunities);
        } catch (Exception e) {
            // Nếu có lỗi, truyền danh sách rỗng
            model.addAttribute("latestOpportunities", List.of());
        }
        return "home/home";
    }

    @GetMapping("/home")
    public String homeAlternative(Model model) {
        // Route thay thế cho trang chủ - sử dụng cùng logic với home
        return home(model);
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
    public String opportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        try {
            // Tạo Pageable
            Pageable pageable = PageRequest.of(page, size);

            // Lấy dữ liệu
            Page<OpportunityCardDto> opportunityPage;
            if (categoryId != null || location != null || status != null || search != null) {
                opportunityPage = opportunityService.getOpportunityCardsWithFilters(
                        categoryId, location, status, search, sort, pageable);
            } else {
                opportunityPage = opportunityService.getOpportunityCards(pageable);
            }

            // Lấy danh sách categories cho filter
            List<Category> categories = opportunityService.getCategoriesWithOpportunities();

            // Thêm vào model
            model.addAttribute("opportunities", opportunityPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", opportunityPage.getTotalPages());
            model.addAttribute("totalElements", opportunityPage.getTotalElements());
            model.addAttribute("hasNext", opportunityPage.hasNext());
            model.addAttribute("hasPrevious", opportunityPage.hasPrevious());

            // Filter parameters
            model.addAttribute("categories", categories);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedLocation", location);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("sortBy", sort);

        } catch (Exception e) {
            // Log error và hiển thị trang trống
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("opportunities", List.of());
            model.addAttribute("categories", List.of());
            model.addAttribute("totalElements", 0);
        }

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
