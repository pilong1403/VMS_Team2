package com.fptuni.vms.controller;

import com.fptuni.vms.model.FAQ;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.FAQService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Controller
public class FAQController {
    private final FAQService faqService;

    public FAQController(FAQService faqService) {
        this.faqService = faqService;
    }

    @GetMapping("/admin/faq")
    public String showFAQPage(Model model,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "5") int size) {

        List<FAQ> listFaqList = faqService.getAllFAQs(page, size);

        long totalFAQs = faqService.getTotalFAQs();
        int totalPages = (int) Math.ceil((double) totalFAQs / size);

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);


        model.addAttribute("listFaqList", listFaqList);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("activePage", "faq");

        return "admin/FAQ";
    }

    @PostMapping("/admin/faq/add")
    public String addFAQ(@RequestParam ("category") String category,
                         @RequestParam ("question") String question,
                         @RequestParam ("answer") String answer,
                         RedirectAttributes redirectAttributes) {

        FAQ faq = new FAQ();
        faq.setCategory(category);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setStatus(true);
        faq.setCreatedAt(java.time.LocalDateTime.now());
        faq.setUpdatedAt(null);
        faq.setUpdatedBy(null);

        if(faqService.createFAQ(faq) == null) {
            redirectAttributes.addFlashAttribute("error", "FAQ added failed !! This question already exists");
            return "redirect:/admin/faq";
        }
        redirectAttributes.addFlashAttribute("success", "FAQ added successfully !!");
        return "redirect:/admin/faq";
    }

    @PostMapping("/admin/faq/edit")
    public String editFAQ(@RequestParam ("faqIdEdit") Integer faqId,
                          @RequestParam ("editedCategory") String editedCategory,
                          @RequestParam ("question") String question,
                          @RequestParam ("answer") String answer,
                          @RequestParam("page") int page,
                          @RequestParam( required = false) Integer num,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(name = "originalCategory", required = false) String originalCategory,
                          @AuthenticationPrincipal CustomUserDetails loggedInUser,
                          RedirectAttributes redirectAttributes) {

        if(!faqService.existsById(faqId)) {
            redirectAttributes.addFlashAttribute("error", "FAQ not found !!");
        } else{
            FAQ existingFAQ = faqService.getFAQById(faqId);
            existingFAQ.setCategory(editedCategory);
            existingFAQ.setQuestion(question);
            existingFAQ.setAnswer(answer);
            existingFAQ.setUpdatedAt(java.time.LocalDateTime.now());

            if (loggedInUser != null) {
                User user = loggedInUser.getUser();
                existingFAQ.setUpdatedBy(user);
            } else{
                existingFAQ.setUpdatedBy(null);
            }

            if(faqService.updateFAQ(existingFAQ) == null) {
                redirectAttributes.addFlashAttribute("error", "FAQ updated failed !! This question already exists");
            } else {
                redirectAttributes.addFlashAttribute("success", "FAQ updated successfully !!");
            }
        }

        if(num != null) {
            String redirectUrl = UriComponentsBuilder.fromPath("/admin/faq/filter")
                    .queryParam("page", page)
                    .queryParam("num", num)
                    .queryParam("category", originalCategory)
                    .queryParam("status", status)
                    .queryParam("keyword", keyword)
                    .toUriString();
            return "redirect:" + redirectUrl;
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/faq/filter")
                .queryParam("page", page)
                .queryParam("status", status)
                .queryParam("category", originalCategory)
                .queryParam("keyword", keyword)
                .toUriString();

        return "redirect:" + redirectUrl;
    }

    @PostMapping("/admin/faq/changeStatus")
    public String changeFAQStatus(@RequestParam ("faqIdStatus") Integer faqId,
                                  @RequestParam("page") int page,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) Integer num,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String keyword,
                                  @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                  RedirectAttributes redirectAttributes) {
        if (!faqService.existsById(faqId)) {
            redirectAttributes.addFlashAttribute("error", "FAQ not found !!");
        } else{
            FAQ existingFAQ = faqService.getFAQById(faqId);
            existingFAQ.setStatus(!existingFAQ.getStatus());
            existingFAQ.setUpdatedAt(java.time.LocalDateTime.now());

            if (loggedInUser != null) {
                User user = loggedInUser.getUser();
                existingFAQ.setUpdatedBy(user);
            } else{
                existingFAQ.setUpdatedBy(null);
            }

            faqService.updateFAQ(existingFAQ);
            redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái FAQ thành công !!");
        }


        if(num != null) {
            String redirectUrl = UriComponentsBuilder.fromPath("/admin/faq/filter")
                    .queryParam("page", page)
                    .queryParam("num", num)
                    .queryParam("category", category)
                    .queryParam("status", status)
                    .queryParam("keyword", keyword)
                    .toUriString();
            return "redirect:" + redirectUrl;
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/faq/filter")
                .queryParam("page", page)
                .queryParam("status", status)
                .queryParam("category", category)
                .queryParam("keyword", keyword)
                .toUriString();

        return "redirect:" + redirectUrl;
    }


    @GetMapping("/admin/faq/filter")
    public String filterFAQ(
            Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer num,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page
            ){

        int recordsPerPage = (num != null && num > 0) ? num : 5;

        List<FAQ> listFaqList = faqService.filterFAQs(status,category, num, keyword != null ? keyword.trim() : null, page, recordsPerPage);

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
            return "admin/FAQ";
        }

        long totalFAQs = faqService.countFilteredFAQs(status, category, keyword);
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

        return "admin/FAQ";
    }

}
