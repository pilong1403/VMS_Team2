package com.fptuni.vms.controller;

import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.service.SupportTicketService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

@Controller
public class SupportTicketController {
    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/admin/support")
    public String showSupportTicketsPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "5") int size,
                                         Model model) {

        long totalTickets = supportTicketService.findAll().size();
        int totalPages = (int) Math.ceil((double) totalTickets / size);

        if (totalPages == 0) {
            totalPages = 1;
        }

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);

        model.addAttribute("listSupportTickets", supportTicketService.findAllWithPagination(page, size));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("activePage", "support");

        // Dữ liệu mặc định cho tab 'Đơn phản hồi'
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("listSupportResponses", Collections.emptyList());
        model.addAttribute("totalPagesResponses", 1);
        model.addAttribute("currentPageResponse", 1);
        model.addAttribute("startPageResponse", 1);
        model.addAttribute("endPageResponse", 1);
        model.addAttribute("numResponse", size);
        model.addAttribute("keywordResponse", "");



        return "admin/Support";
    }

    @GetMapping("/admin/support/filter")
    public String filterSupportTickets(Model model,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String priority,
                                       @RequestParam(required = false) Integer num,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") int page) {

        int recordsPerPage = (num != null && num > 0) ? num : 5;
        List<SupportTicket> ticketListFiltered = supportTicketService.filterTickets(status, priority, num, keyword, page, recordsPerPage);

        if (ticketListFiltered == null || ticketListFiltered.isEmpty()) {
            model.addAttribute("listSupportTickets", java.util.Collections.emptyList());
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", 1);
            model.addAttribute("status", null);
            model.addAttribute("num", null);
            model.addAttribute("keyword", keyword);
            model.addAttribute("priority", null);
            model.addAttribute("error", "There are no support tickets found !!");
            model.addAttribute("activePage", "support");
            return "admin/Support";
        }

        long totalFilteredTickets = supportTicketService.countFilteredTickets(status, priority, keyword);
        int totalPages = (int) Math.ceil((double) totalFilteredTickets / recordsPerPage);

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);


        // Dữ liệu mặc định cho tab 'Đơn phản hồi'
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("listSupportResponses", Collections.emptyList());
        model.addAttribute("totalPagesResponses", 1);
        model.addAttribute("currentPageResponse", 1);
        model.addAttribute("startPageResponse", 1);
        model.addAttribute("endPageResponse", 1);
        model.addAttribute("numResponse", num);
        model.addAttribute("keywordResponse", "");
        // --------------------------------------------------------

        model.addAttribute("listSupportTickets", ticketListFiltered);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("status", status);
        model.addAttribute("num", num);
        model.addAttribute("priority", priority);
        model.addAttribute("keyword", keyword.trim());

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("activePage", "support");

        return "admin/Support";

    }
}
