package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.VolunteerRatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/ratings")
public class VolunteerRatingController {

    @Autowired
    private VolunteerRatingService ratingService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OpportunityService opportunityService;
    @Autowired
    private UserService userService;

    /**
     * Lấy user đang đăng nhập từ session
     */
    private User getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("AUTH_USER_ID");
        if (userId == null) return null;
        return userService.getUserById(userId);
    }

    private boolean hasRole(HttpSession session, String role) {
        String r = (String) session.getAttribute("AUTH_ROLE");
        return role.equals(r);
    }

    // ===== LIST =====
    @GetMapping
    public String listRatings(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              @RequestParam(required = false) Short stars,
                              @RequestParam(required = false) String keyword,
                              Model model,
                              HttpSession session) {

        // Kiểm tra đăng nhập
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";

        // Kiểm tra role
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        // Lấy tổ chức
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        List<VolunteerRating> ratings = ratingService.findByOrganization(org.getOrgId(), keyword, stars, page, size);
        long total = ratingService.countByOrganization(org.getOrgId(), keyword, stars);
        int totalPages = (int) Math.ceil((double) total / size);

        model.addAttribute("ratings", ratings);
        model.addAttribute("pendingCount", ratingService.countPending(org.getOrgId()));
        model.addAttribute("doneCount", ratingService.countDone(org.getOrgId()));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("filteredStars", stars);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);
        return "rating/listRatingVolunteer";
    }

    // ===== CREATE =====
    @GetMapping("/create")
    public String createForm(Model model, HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        model.addAttribute("rating", new VolunteerRating());
        model.addAttribute("opportunities", opportunityService.findByOrganization(org.getOrgId()));
        model.addAttribute("volunteers", userService.getUsersByRole(3)); // Role 3 = VOLUNTEER
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);
        return "rating/rateVolunteer";
    }

    // ===== SAVE =====
    @PostMapping("/save")
    public String saveRating(@ModelAttribute VolunteerRating rating, HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        Opportunity opp = opportunityService.findById(rating.getOpportunity().getOppId());
        User ratee = userService.getUserById(rating.getRateeUser().getUserId());

        rating.setOpportunity(opp);
        rating.setRateeUser(ratee);
        rating.setRaterOrg(org);
        rating.setUpdatedAt(LocalDateTime.now());

        ratingService.save(rating);
        return "redirect:/ratings";
    }

    // ===== EDIT =====
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable int id, Model model, HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        VolunteerRating rating = ratingService.findById(id);
        if (rating == null) return "redirect:/ratings";

        model.addAttribute("rating", rating);
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);
        return "rating/updateRate";
    }

    // ===== UPDATE =====
    @PostMapping("/update")
    public String updateRating(@ModelAttribute VolunteerRating rating, HttpSession session) {
        User me = getCurrentUser(session);
        if (me == null) return "redirect:/login";
        if (!hasRole(session, "ORG_OWNER")) return "redirect:/403";

        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        VolunteerRating existing = ratingService.findById(rating.getId());
        if (existing == null) return "redirect:/ratings";

        existing.setStars(rating.getStars());
        existing.setComment(rating.getComment());
        existing.setUpdatedAt(LocalDateTime.now());
        ratingService.update(existing);

        return "redirect:/ratings";
    }
}
