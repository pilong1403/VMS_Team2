package com.fptuni.vms.controller;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.VolunteerRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    // ===== LIST =====
    @GetMapping
    public String listRatings(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              @RequestParam(required = false) Short stars,
                              @RequestParam(required = false) String keyword,
                              Model model,
                              Principal principal) {

        String email = (principal != null) ? principal.getName() : "quynhmai@example.vn";
        User me = userService.findByEmail(email);
        if (me == null) {
            model.addAttribute("errorMessage", "Không tìm thấy tài khoản người dùng: " + email);
            return "error/unauthorized";
        }

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
    public String createForm(Model model, Principal principal) {
        String email = (principal != null) ? principal.getName() : "quynhmai@example.vn";
        User me = userService.findByEmail(email);
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        model.addAttribute("rating", new VolunteerRating());
        model.addAttribute("opportunities", opportunityService.findByOrganization(org.getOrgId()));
        model.addAttribute("volunteers", userService.getUsersByRole(3)); // roleId=3 => VOLUNTEER
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);
        return "rating/rateVolunteer";
    }

    // ===== SAVE =====
    @PostMapping("/save")
    public String saveRating(@ModelAttribute VolunteerRating rating, Principal principal) {
        String email = (principal != null) ? principal.getName() : "quynhmai@example.vn";
        User me = userService.findByEmail(email);
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) return "redirect:/access-denied";

        // lấy entity thật từ id
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
    public String editForm(@PathVariable int id, Model model, Principal principal) {
        String email = (principal != null) ? principal.getName() : "quynhmai@example.vn";
        User me = userService.findByEmail(email);
        Organization org = organizationService.findByOwnerId(me.getUserId());
        VolunteerRating rating = ratingService.findById(id);
        if (rating == null) return "redirect:/ratings";

        model.addAttribute("rating", rating);
        model.addAttribute("currentUser", me);
        model.addAttribute("currentOrg", org);
        return "rating/updateRate";
    }

    // ===== UPDATE =====
    @PostMapping("/update")
    public String updateRating(@ModelAttribute VolunteerRating rating, Principal principal) {
        String email = (principal != null) ? principal.getName() : "quynhmai@example.vn";
        User me = userService.findByEmail(email);
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
