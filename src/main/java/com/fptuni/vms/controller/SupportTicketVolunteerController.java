package com.fptuni.vms.controller;

import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.CloudinaryService;
import com.fptuni.vms.service.SupportTicketService;
import com.fptuni.vms.service.UserService;
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
public class SupportTicketVolunteerController {

    private final SupportTicketService supportTicketService;
    private CloudStorageService CloudStorageService;

    public SupportTicketVolunteerController(SupportTicketService supportTicketService, CloudStorageService CloudStorageService) {
        this.supportTicketService = supportTicketService;
        this.CloudStorageService = CloudStorageService;
    }

    @GetMapping("/support_ticket")
    public String viewSupportTicketPage(Model model, @AuthenticationPrincipal CustomUserDetails loggedInUser) {
        model.addAttribute("activeTab", "submit");
        model.addAttribute("activePage", "support_ticket");
        model.addAttribute("user", loggedInUser.getUser());// sd để hiển thị tên user trên sidebar


        // add data default for support ticket tab (tab2)
        model.addAttribute("listSupportTickets", java.util.Collections.emptyList());
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", 1);
        model.addAttribute("status", null);
        model.addAttribute("num", null);
        model.addAttribute("keyword", "");
        model.addAttribute("priority", null);
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);

        //add data default for response ticket tab (tab3)
        model.addAttribute("listSupportResponses", Collections.emptyList());
        model.addAttribute("totalPagesResponses", 1);
        model.addAttribute("currentPageResponse", 1);
        model.addAttribute("startPageResponse", 1);
        model.addAttribute("endPageResponse", 1);
        model.addAttribute("numResponse", 0);
        model.addAttribute("keywordResponse", "");

        return "support/SupportVolunteer";

    }

    @GetMapping("/support_ticket_tab2")
    public String showSupportTicketPage(Model model,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String priority,
                                        @RequestParam(required = false) Integer num,
                                        @RequestParam(required = false) String keyword,
                                        @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                        @RequestParam(defaultValue = "1") int page) {
        model.addAttribute("activePage", "support_ticket");
        model.addAttribute("user", loggedInUser.getUser());// sd để hiển thị tên user trên sidebar
        int userId = loggedInUser.getUserId();
        int recordsPerPage = (num != null && num > 0) ? num : 5;
        List<SupportTicket> ticketListFiltered = supportTicketService.filterTicketsWithUserId(status, priority, num, userId, keyword, page, recordsPerPage);

        if (ticketListFiltered == null || ticketListFiltered.isEmpty()) {
            model.addAttribute("listSupportTickets", java.util.Collections.emptyList());
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", 1);
            model.addAttribute("status", null);
            model.addAttribute("num", null);
            model.addAttribute("keyword", keyword);
            model.addAttribute("priority", null);
            model.addAttribute("error", "There are no support tickets found !!");
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 1);
            model.addAttribute("activeTab", "ticket");

            // Dữ liệu mặc định cho tab 'Đơn phản hồi'
            model.addAttribute("listSupportResponses", Collections.emptyList());
            model.addAttribute("totalPagesResponses", 1);
            model.addAttribute("currentPageResponse", 1);
            model.addAttribute("startPageResponse", 1);
            model.addAttribute("endPageResponse", 1);
            model.addAttribute("numResponse", num);
            model.addAttribute("keywordResponse", "");

            return "support/SupportVolunteer";
        }

        long totalFilteredTickets = supportTicketService.countFilteredTicketsWithUserId(userId, status, priority, keyword);
        int totalPages = (int) Math.ceil((double) totalFilteredTickets / recordsPerPage);

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);


        // Dữ liệu mặc định cho tab 'Đơn phản hồi'
        model.addAttribute("listSupportResponses", Collections.emptyList());
        model.addAttribute("totalPagesResponses", 1);
        model.addAttribute("currentPageResponse", 1);
        model.addAttribute("startPageResponse", 1);
        model.addAttribute("endPageResponse", 1);
        model.addAttribute("numResponse", num);
        model.addAttribute("keywordResponse", "");
        // --------------------------------------------------------

        model.addAttribute("activeTab", "ticket");
        model.addAttribute("listSupportTickets", ticketListFiltered);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("status", status);
        model.addAttribute("num", num);
        model.addAttribute("priority", priority);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : null);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "support/SupportVolunteer";
    }

    @PostMapping("/support_ticket_tab2/update")
    public String updateSupportTicketStatus(@RequestParam("ticketId") Integer ticketId,
                                            @RequestParam("titleUpdate") String ticketTitle,
                                            @RequestParam("priorityUpdate") String ticketPriority,
                                            @RequestParam("desUpdate") String ticketDes,
                                            @RequestParam("attachmentUpdate") MultipartFile attachment,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "5") int size,
                                            RedirectAttributes redirectAttributes) {

        Optional<SupportTicket> optionalTicket = supportTicketService.findById(ticketId);

        if (optionalTicket.isPresent()) {
            SupportTicket ticket = optionalTicket.get();
            ticket.setSubject(ticketTitle.trim());
            ticket.setPriority(SupportTicket.TicketPriority.valueOf(ticketPriority));
            ticket.setDescription(ticketDes.trim());

            if(attachment != null && !attachment.isEmpty()) {
                 String url = CloudStorageService.uploadFile(attachment);
                if(url == null){
                    redirectAttributes.addFlashAttribute("error", "Loại file không hợp lệ !! Cập nhật đơn #" + ticketId + " thất bại !!");
                    String redirectUrl = UriComponentsBuilder.fromPath("/support_ticket_tab2")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .toUriString();
                    return "redirect:" + redirectUrl;
                }
                ticket.setAttachmentUrl(url);
            }

            supportTicketService.update(ticket);

            redirectAttributes.addFlashAttribute("success", "Cập nhật đơn yêu cầu #" + ticketId + " thành công !!");
            String redirectUrl = UriComponentsBuilder.fromPath("/support_ticket_tab2")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .toUriString();
            return "redirect:" + redirectUrl;
        }

        redirectAttributes.addFlashAttribute("error", "Cập nhật đơn yêu cầu #" + ticketId + " thất bại !!");
        String redirectUrl = UriComponentsBuilder.fromPath("/support_ticket_tab2")
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        return "redirect:" + redirectUrl;
    }


    @PostMapping("/support_ticket/create")
    public String createSupportTicket(@RequestParam("title") String ticketTitle,
                                      @RequestParam("priority") String ticketPriority,
                                      @RequestParam("description") String ticketDes,
                                      @RequestParam("attachment") MultipartFile attachment,
                                      @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                      RedirectAttributes redirectAttributes) {
        SupportTicket newTicket = new SupportTicket();

        newTicket.setSubject(ticketTitle.trim());
        newTicket.setContactEmail(loggedInUser.getEmail());
        newTicket.setPriority(SupportTicket.TicketPriority.valueOf(ticketPriority.toUpperCase()));
        newTicket.setDescription(ticketDes.trim());
        newTicket.setUser(loggedInUser.getUser());
        newTicket.setStatus(SupportTicket.TicketStatus.OPEN);
        newTicket.setUpdatedAt(null);
        newTicket.setResolvedBy(null);

        if(attachment != null && !attachment.isEmpty()) {
            String url = CloudStorageService.uploadFile(attachment);
            if(url == null){
                redirectAttributes.addFlashAttribute("error", "Loại file không hợp lệ !! Tạo đơn yêu cầu thất bại !!");
                return "redirect:/support_ticket";
            }
            newTicket.setAttachmentUrl(url);
        }

        supportTicketService.create(newTicket);
        redirectAttributes.addFlashAttribute("success", "Tạo đơn yêu cầu thành công !!");
        return "redirect:/support_ticket";
    }




}
