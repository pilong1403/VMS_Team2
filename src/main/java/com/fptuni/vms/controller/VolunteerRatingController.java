package com.fptuni.vms.controller;

import com.fptuni.vms.dto.response.OpportunitySummaryDto;
import com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.VolunteerRatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/organization/ratings")
public class VolunteerRatingController {

    @Autowired
    private VolunteerRatingService ratingService;
    @Autowired
    private OpportunityService opportunityService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;

    // ====== Helper ======
    private User getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("AUTH_USER_ID");
        return userId == null ? null : userService.getUserById(userId);
    }

    private boolean hasRole(HttpSession session, String role) {
        String r = (String) session.getAttribute("AUTH_ROLE");
        return role.equals(r);
    }

    private Organization getCurrentOrg(User me) {
        return organizationService.findByOwnerId(me.getUserId());
    }

    // ===================== 1. DANH SÁCH HOẠT ĐỘNG (CÓ FILTER) =====================
    @GetMapping("/opportunities")
    public String listOpportunities(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "all") String eventStatus, // all, upcoming, ongoing, finished
                                    @RequestParam(defaultValue = "recent") String sort,     // recent, oldest, name, participants
                                    Model model, HttpSession session) {

        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";
        Organization org = getCurrentOrg(me);
        if (org == null) return "redirect:/access-denied";

        int offset = page * size;
        List<OpportunitySummaryDto> opportunities = ratingService.findOpportunitiesByOrg(
                org.getOrgId(), keyword, eventStatus, sort, offset, size);
        long total = ratingService.countOpportunitiesByOrg(org.getOrgId(), keyword, eventStatus);
        int totalPages = (int) Math.ceil((double) total / size);

        // Gửi data ra view
        model.addAttribute("opportunities", opportunities);
        model.addAttribute("keyword", keyword);
        model.addAttribute("eventStatus", eventStatus);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);

        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);

        return "rating/listOpportunities";
    }

    // ===================== 2. DANH SÁCH TNV TRONG HOẠT ĐỘNG =====================
    @GetMapping("/opportunity/{oppId}")
    public String listVolunteersByOpportunity(@PathVariable int oppId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "all") String statusFilter, // all, pending, rated, not_attended
                                              @RequestParam(defaultValue = "name") String sort, // name, hours, checkin
                                              HttpSession session,
                                              Model model) {

        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";
        Organization org = getCurrentOrg(me);
        if (org == null) return "redirect:/access-denied";

        int offset = page * size;
        List<OpportunityVolunteerRatingDto> volunteers = ratingService.getVolunteersForOpportunity(
                org.getOrgId(), oppId, keyword, statusFilter, sort, offset, size);
        long total = ratingService.countVolunteersForOpportunity(org.getOrgId(), oppId, keyword);
        int totalPages = (int) Math.ceil((double) total / size);

        model.addAttribute("volunteers", volunteers);
        model.addAttribute("opportunity", opportunityService.findById(oppId));
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);

        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);

        return "rating/listRatingVolunteer";
    }
    // ===================== 4. FORM CHỈNH SỬA RATING =====================
    @GetMapping("/edit/{id}")
    public String editRating(@PathVariable int id, HttpSession session, Model model) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        VolunteerRating rating = ratingService.findById(id);
        if (rating == null) {
            return "redirect:/organization/ratings";
        }

        // Lấy thông tin user & opportunity
        User volunteer = rating.getRateeUser();
        Opportunity opp = rating.getOpportunity();

        // Tính điểm trung bình của TNV
        double avgStars = ratingService.getAverageStarsByUser(volunteer.getUserId());

        model.addAttribute("rating", rating);
        model.addAttribute("volunteer", volunteer);
        model.addAttribute("opportunity", opp);
        model.addAttribute("avgStars", avgStars);
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", getCurrentOrg(me));

        return "rating/updateRate";
    }

    // ===================== 3. HIỂN THỊ FORM THÊM RATING MỚI =====================
    @GetMapping("/save/{oppId}/{userId}")
    public String showCreateRatingForm(@PathVariable int oppId,
                                       @PathVariable int userId,
                                       HttpSession session,
                                       Model model) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Opportunity opp = opportunityService.findById(oppId);
        User volunteer = userService.getUserById(userId);

        double avgStars = ratingService.getAverageStarsByUser(userId);

        VolunteerRating rating = new VolunteerRating();
        model.addAttribute("rating", rating);
        model.addAttribute("opportunity", opp);
        model.addAttribute("volunteer", volunteer);
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", getCurrentOrg(me));
        model.addAttribute("avgStars", avgStars);
        return "rating/rateVolunteer";
    }

    // ===================== 4. CẬP NHẬT RATING =====================
    @PostMapping("/update")
    public String updateRating(@RequestParam int oppId,
                               @ModelAttribute VolunteerRating rating,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        try {
            VolunteerRating existing = ratingService.findById(rating.getId());
            existing.setStars(rating.getStars());
            existing.setComment(rating.getComment());
            existing.setUpdatedAt(LocalDateTime.now());
            ratingService.update(existing);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại!");
        }

        return "redirect:/organization/ratings/opportunity/" + oppId;
    }



    @PostMapping("/save")
    public String saveRating(@RequestParam int oppId,
                             @RequestParam int userId,
                             @ModelAttribute VolunteerRating rating,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Organization org = getCurrentOrg(me);

        try {
            ratingService.createRating(oppId, userId, org.getOrgId(), rating);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá đã được lưu thành công!");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Không thể lưu đánh giá!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi lưu đánh giá!");
        }

        return "redirect:/organization/ratings/opportunity/" + oppId;
    }



}
