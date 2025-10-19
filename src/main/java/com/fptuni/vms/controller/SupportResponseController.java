package com.fptuni.vms.controller;

import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.SupportResponse;
import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.SupportResponseService;
import com.fptuni.vms.service.SupportTicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class SupportResponseController {

    private final SupportResponseService supportResponseService;
    private final SupportTicketService supportTicketService;

    public SupportResponseController(SupportResponseService supportResponseService, SupportTicketService supportTicketService) {
        this.supportResponseService = supportResponseService;
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/admin/support-responses")
    public String showSupportResponses(
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer num,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        int pageSize = (num != null && num > 0) ? num : size;
        List<SupportResponse> responseList = supportResponseService.filterAndPaginateResponses(keyword, num, page, pageSize);

        if (responseList == null || responseList.isEmpty()) {
            model.addAttribute("listSupportResponses", Collections.emptyList());
            model.addAttribute("totalPagesResponses", 1);
            model.addAttribute("currentPageResponse", 1);
            model.addAttribute("startPageResponse", 1);
            model.addAttribute("endPageResponse", 1);
            model.addAttribute("keywordResponse", keyword);
            model.addAttribute("numResponse", num);
            model.addAttribute("activeTab", "responses");
            model.addAttribute("errorResponse", "There are no support responses found !!");
        } else {
            long totalResponses = supportResponseService.countFilteredResponses(keyword);
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
            model.addAttribute("activeTab", "responses");
            model.addAttribute("activePage", "support");
        }

        // data mặc định cho tab support tickets
        model.addAttribute("listSupportTickets", Collections.emptyList());
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", 1);
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);
        model.addAttribute("status", "");
        model.addAttribute("priority", "");
        model.addAttribute("keyword", "");
        model.addAttribute("num", size);

        return "admin/Support";
    }

    @PostMapping("/admin/support/add-respond")
    public String addSupportResponse(@RequestParam("ticketId") int ticketId,
                                     @RequestParam("message") String message,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam("attachment") MultipartFile attachment,
                                     @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                     RedirectAttributes redirectAttributes
                                     ) {

        User admin = loggedInUser.getUser();
        boolean isSuccess = supportResponseService.addAdminResponse(ticketId, message, admin, attachment);

        if (isSuccess) {
            redirectAttributes.addFlashAttribute("success", "Phản hồi đơn hỗ trợ #" + ticketId + " thành công !!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Phản hồi đơn hỗ trợ thất bại, có lỗi đã xảy ra. Vui lòng thử lại !!");
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/support")
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/admin/support/mark-resolved")
    public String markTicketAsResolved(@RequestParam("ticketId") int ticketId,
                                       @RequestParam("status") String status,
                                       @RequestParam("priority") String priority,
                                       @RequestParam("keyword") String keyword,
                                       @RequestParam("num") Integer num,
                                       @RequestParam(defaultValue = "1") int page,
                                       RedirectAttributes redirectAttributes) {

        Optional<SupportTicket> optionalTicket = supportTicketService.findById(ticketId);
        if (optionalTicket.isPresent()) {
            SupportTicket ticket = optionalTicket.get();

            if(ticket.getStatus() == SupportTicket.TicketStatus.OPEN) {
                redirectAttributes.addFlashAttribute("error", "Hãy phản hồi đơn hỗ trợ trước khi đánh dấu !!");
                String redirectUrl = UriComponentsBuilder.fromPath("/admin/support/filter")
                        .queryParam("page", page)
                        .queryParam("num", num)
                        .queryParam("status", status)
                        .queryParam("priority", priority)
                        .queryParam("keyword", keyword)
                        .toUriString();
                return "redirect:" + redirectUrl;
            }

            ticket.setStatus(SupportTicket.TicketStatus.CLOSED);
            supportTicketService.update(ticket);
            redirectAttributes.addFlashAttribute("success", "Đánh dấu đã xử lý đơn hỗ trợ #" + ticketId + " thành công !!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ticket not found.");
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/support/filter")
                .queryParam("page", page)
                .queryParam("num", num)
                .queryParam("status", status)
                .queryParam("priority", priority)
                .queryParam("keyword", keyword)
                .toUriString();
        return "redirect:" + redirectUrl;
    }

}
