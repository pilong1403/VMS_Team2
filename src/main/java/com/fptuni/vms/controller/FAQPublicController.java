package com.fptuni.vms.controller;

import com.fptuni.vms.model.FAQ;
import com.fptuni.vms.service.FAQService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FAQPublicController {
    private final FAQService faqService;

    public FAQPublicController(FAQService faqService) {
        this.faqService = faqService;
    }

    @GetMapping("/faqPublic")
    public String showFAQPublicPage( Model model,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Integer num,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(defaultValue = "1") int page) {

        int recordsPerPage = 5;

        List<FAQ> listFaqList = faqService.filterFAQsPublic(category, num, keyword != null ? keyword.trim() : null, page, recordsPerPage);

        if (listFaqList == null || listFaqList.isEmpty()) {
            model.addAttribute("listFaqList", java.util.Collections.emptyList());
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 1);
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 0);
            model.addAttribute("status", status);
            model.addAttribute("num", num);
            model.addAttribute("category", category);
            model.addAttribute("keyword", keyword);
            model.addAttribute("error", "There are no FAQs found !!");
            return "public/faq";
        }

        long totalFAQs = faqService.countFilteredFAQsPublic(category, keyword);
        int totalPages = (int) Math.ceil((double) totalFAQs / recordsPerPage);

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);


        model.addAttribute("listFaqList", listFaqList);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("status", status);
        model.addAttribute("num", num);
        model.addAttribute("category", category);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : null);

        return "public/faq";
    }
}
