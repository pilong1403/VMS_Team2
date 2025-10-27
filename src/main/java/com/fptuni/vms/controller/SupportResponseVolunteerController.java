package com.fptuni.vms.controller;

import com.fptuni.vms.model.SupportResponse;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.SupportResponseService;
import com.fptuni.vms.service.SupportTicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class SupportResponseVolunteerController {
    private final SupportResponseService supportResponseService;
    private final SupportTicketService supportTicketService;

    public SupportResponseVolunteerController(SupportResponseService supportResponseService, SupportTicketService supportTicketService) {
        this.supportResponseService = supportResponseService;
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/support_ticket_tab3")
    public String showSupportResponses(
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer num,
            @AuthenticationPrincipal CustomUserDetails loggedInUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        int userId = loggedInUser.getUser().getUserId();
        int pageSize = (num != null && num > 0) ? num : size;
        List<SupportResponse> responseList = supportResponseService.findResponsesBySenderId(userId, keyword, num, page, pageSize);

        if (responseList == null || responseList.isEmpty()) {
            model.addAttribute("activeTab", "response");
            model.addAttribute("listSupportResponses", Collections.emptyList());
            model.addAttribute("totalPagesResponses", 1);
            model.addAttribute("currentPageResponse", 1);
            model.addAttribute("startPageResponse", 1);
            model.addAttribute("endPageResponse", 1);
            model.addAttribute("keywordResponse", keyword);
            model.addAttribute("numResponse", num);
            model.addAttribute("errorResponse", "There are no support responses found !!");
        } else {
            long totalResponses = supportResponseService.countResponsesBySenderId(userId, keyword);
            int totalPages = (int) Math.ceil((double) totalResponses / pageSize);
            if (totalPages == 0) {
                totalPages = 1;
            }

            int visiblePages = 3;
            int startPage = Math.max(1, page - visiblePages / 2);
            int endPage = Math.min(totalPages, startPage + visiblePages - 1);


            model.addAttribute("listSupportResponses", responseList);
            model.addAttribute("totalPagesResponses", totalPages);
            model.addAttribute("currentPageResponse", page);
            model.addAttribute("startPageResponse", startPage);
            model.addAttribute("endPageResponse", endPage);
            model.addAttribute("keywordResponse", keyword);
            model.addAttribute("numResponse", num);
            model.addAttribute("activeTab", "response");
        }

        // data mặc định cho tab support tickets (tab2)
        model.addAttribute("listSupportTickets", Collections.emptyList());
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", 1);
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);
        model.addAttribute("status", "");
        model.addAttribute("priority", "");
        model.addAttribute("keyword", "");
        model.addAttribute("num", size);

        return "support/SupportVolunteer";
    }



}
