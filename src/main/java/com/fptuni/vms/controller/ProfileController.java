package com.fptuni.vms.controller;

import com.fptuni.vms.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fptuni.vms.model.User;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.dto.response.ChangePasswordForm;
import com.fptuni.vms.dto.response.ProfileForm;
import com.fptuni.vms.dto.VolunteerRatingDto;
import com.fptuni.vms.dto.ScheduleApplicationDto;
import com.fptuni.vms.dto.VolunteerScheduleResponseDto;
import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.service.UserService;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.FeedbackService;
import com.fptuni.vms.service.CategoryService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page; // import thêm để dùng Page

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final ApplicationService applicationService;
    private final FeedbackService feedbackService;
    private final CategoryService categoryService;

    public ProfileController(UserService userService, ApplicationService applicationService,
            FeedbackService feedbackService, CategoryService categoryService) {
        this.userService = userService;
        this.applicationService = applicationService;
        this.feedbackService = feedbackService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String viewProfile(Model model, Authentication authentication) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", freshUser);
        model.addAttribute("activePage", "profile");

        if ("Volunteer".equals(freshUser.getRole().getRoleName())) {
            VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
            model.addAttribute("volunteerRating", rating);
        }

        return "profile/view";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Authentication authentication) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        ProfileForm profileForm = new ProfileForm();
        profileForm.setFullName(freshUser.getFullName());
        profileForm.setEmail(freshUser.getEmail());
        profileForm.setPhone(freshUser.getPhone());
        profileForm.setAddress(freshUser.getAddress());

        model.addAttribute("user", freshUser);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("activePage", "settings");

        if ("Volunteer".equals(freshUser.getRole().getRoleName())) {
            VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
            model.addAttribute("volunteerRating", rating);
        }

        return "profile/edit";
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Valid @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập đã hết hạn");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            userService.updateProfile(currentUser.getUserId(), profileForm, avatarFile);

            response.put("success", true);
            response.put("message", "Cập nhật thông tin cá nhân thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật thông tin: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update-form")
    public String updateProfileForm(
            @Valid @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", profileForm);

                if ("Volunteer".equals(currentUser.getRole().getRoleName())) {
                    VolunteerRatingDto rating = new VolunteerRatingDto(4.8, 25);
                    model.addAttribute("volunteerRating", rating);
                }

                return "profile/edit";
            }

            userService.updateProfile(currentUser.getUserId(), profileForm, avatarFile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công");
            return "redirect:/profile";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật thông tin: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    @PostMapping("/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @ModelAttribute ChangePasswordForm changePasswordForm,
            BindingResult bindingResult,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            User currentUser = SecurityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập đã hết hạn");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Additional validation for password confirmation
            if (!changePasswordForm.isPasswordsMatch()) {
                Map<String, String> errors = new HashMap<>();
                errors.put("confirmPassword", "Mật khẩu xác nhận không khớp");
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Change password
            userService.changePassword(currentUser.getUserId(), changePasswordForm);

            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-schedule")
    public String mySchedule(
            Model model,
            Authentication authentication,
            @RequestParam(name = "view", defaultValue = "calendar") String view,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "category", defaultValue = "") String filterCategory,
            @RequestParam(name = "sort", defaultValue = "earliest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        User currentUser = SecurityUtils.getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/login";
        }

        // Check if user is volunteer role
        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "redirect:/";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        // Get volunteer's applications
        List<Application> allApplications = applicationService.listMyApplications(currentUser.getUserId());

        // Separate upcoming and past events based on opportunity time
        LocalDateTime now = LocalDateTime.now();

        // All upcoming for calendar view (no filter)
        List<ScheduleApplicationDto> allUpcomingForCalendar = allApplications.stream()
                .filter(app -> app.getOpportunity().getStartTime().isAfter(now))
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED)
                .map(this::convertToScheduleDto)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Filtered upcoming for list view
        List<ScheduleApplicationDto> filteredUpcoming = allApplications.stream()
                .filter(app -> app.getOpportunity().getStartTime().isAfter(now))
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED)
                .filter(app -> {
                    // Search filter
                    if (!q.isEmpty()) {
                        String searchLower = q.toLowerCase();
                        boolean matchTitle = app.getOpportunity().getTitle().toLowerCase().contains(searchLower);
                        boolean matchLocation = app.getOpportunity().getLocation() != null &&
                                app.getOpportunity().getLocation().toLowerCase().contains(searchLower);
                        return matchTitle || matchLocation;
                    }
                    return true;
                })
                .filter(app -> {
                    // Category filter
                    if (!filterCategory.isEmpty()) {
                        try {
                            int catId = Integer.parseInt(filterCategory);
                            return app.getOpportunity().getCategory() != null &&
                                    app.getOpportunity().getCategory().getCategoryId() == catId;
                        } catch (NumberFormatException e) {
                            return true;
                        }
                    }
                    return true;
                })
                .map(this::convertToScheduleDto)
                .collect(Collectors.toList());

        // Sort
        if ("latest".equals(sort)) {
            filteredUpcoming.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
        } else {
            filteredUpcoming.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        }

        // Pagination for list view
        int pageSize = 5;
        int totalItems = filteredUpcoming.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<ScheduleApplicationDto> pagedItems = new ArrayList<>();
        if (startIndex < totalItems) {
            pagedItems = filteredUpcoming.subList(startIndex, endIndex);
        }

        // Calculate statistics
        long completedCount = allApplications.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.COMPLETED)
                .count();

        long totalHours = allApplications.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.APPROVED ||
                        app.getStatus() == Application.ApplicationStatus.COMPLETED)
                .mapToLong(app -> {
                    LocalDateTime start = app.getOpportunity().getStartTime();
                    LocalDateTime end = app.getOpportunity().getEndTime();
                    return java.time.Duration.between(start, end).toHours();
                })
                .sum();

        // Create response DTO
        VolunteerScheduleResponseDto scheduleResponse = new VolunteerScheduleResponseDto();
        scheduleResponse.setUpcomingApplications(allUpcomingForCalendar);
        scheduleResponse.setPastApplications(new ArrayList<>());
        scheduleResponse.setUpcomingCount(allUpcomingForCalendar.size());
        scheduleResponse.setCompletedCount((int) completedCount);
        scheduleResponse.setTotalHours(totalHours);

        // Get categories for filter dropdown
        model.addAttribute("categories", categoryService.listAll());

        model.addAttribute("user", freshUser);
        model.addAttribute("scheduleData", scheduleResponse);
        model.addAttribute("allUpcomingForCalendar", allUpcomingForCalendar);
        model.addAttribute("items", pagedItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("q", q);
        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);
        model.addAttribute("activePage", "schedule");

        return "volunteer/my-schedual";
    }

    private ScheduleApplicationDto convertToScheduleDto(Application app) {
        ScheduleApplicationDto dto = new ScheduleApplicationDto();
        dto.setAppId(app.getAppId());
        dto.setOpportunityTitle(app.getOpportunity().getTitle());
        dto.setOrganizationName(app.getOpportunity().getOrganization().getName());
        dto.setLocation(app.getOpportunity().getLocation());
        dto.setStartTime(app.getOpportunity().getStartTime());
        dto.setEndTime(app.getOpportunity().getEndTime());
        dto.setAppliedAt(app.getAppliedAt());
        dto.setStatus(app.getStatus().name());
        dto.setNeededVolunteers(app.getOpportunity().getNeededVolunteers());
        dto.setDescription(app.getOpportunity().getSubtitle());
        dto.setThumbnailUrl(app.getOpportunity().getThumbnailUrl());
        return dto;
    }

    // Event History Methods
    @GetMapping("/event-history")
    public String eventHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "opportunity", defaultValue = "") String opportunitySearch,
            @RequestParam(name = "location", defaultValue = "") String locationSearch,
            @RequestParam(name = "category", defaultValue = "") String filterCategory,
            Authentication authentication,
            Model model) {

        User currentUser = SecurityUtils.getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "redirect:/home";
        }

        // Get all event history
        List<EventHistoryDto> allEventHistory = feedbackService.getVolunteerEventHistory(
                currentUser.getUserId(), 0, Integer.MAX_VALUE);

        // Apply filters
        List<EventHistoryDto> filteredHistory = allEventHistory.stream()
                .filter(event -> {
                    // Opportunity search filter
                    if (!opportunitySearch.isEmpty()) {
                        String searchLower = opportunitySearch.toLowerCase();
                        return event.getOpportunityTitle().toLowerCase().contains(searchLower);
                    }
                    return true;
                })
                .filter(event -> {
                    // Location search filter
                    if (!locationSearch.isEmpty()) {
                        String searchLower = locationSearch.toLowerCase();
                        return event.getLocation() != null &&
                                event.getLocation().toLowerCase().contains(searchLower);
                    }
                    return true;
                })
                .filter(event -> {
                    // Category filter
                    if (!filterCategory.isEmpty()) {
                        try {
                            int catId = Integer.parseInt(filterCategory);
                            return event.getCategoryId() != null && event.getCategoryId() == catId;
                        } catch (NumberFormatException e) {
                            return true;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Pagination
        int totalItems = filteredHistory.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);

        List<EventHistoryDto> pagedHistory = new ArrayList<>();
        if (startIndex < totalItems) {
            pagedHistory = filteredHistory.subList(startIndex, endIndex);
        }

        // Calculate statistics
        long totalCompleted = allEventHistory.size();
        long totalAttended = allEventHistory.stream()
                .filter(EventHistoryDto::isHasAttended)
                .count();
        long totalRated = allEventHistory.stream()
                .filter(EventHistoryDto::isHasRated)
                .count();

        // Get categories for filter dropdown
        model.addAttribute("categories", categoryService.listAll());

        model.addAttribute("eventHistory", pagedHistory);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalEvents", totalItems);
        model.addAttribute("totalCompleted", totalCompleted);
        model.addAttribute("totalAttended", totalAttended);
        model.addAttribute("totalRated", totalRated);
        model.addAttribute("opportunitySearch", opportunitySearch);
        model.addAttribute("locationSearch", locationSearch);
        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("user", currentUser);
        model.addAttribute("activePage", "event-history");

        return "volunteer/event-history";
    }

    @PostMapping(value = "/rate-event", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String rateEvent(@RequestParam int oppId,
            @RequestParam int rating,
            @RequestParam(required = false) String content,
            Authentication authentication) {

        User currentUser = SecurityUtils.getCurrentUser(authentication);
        if (currentUser == null) {
            return "error:Vui lòng đăng nhập";
        }

        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "error:Không có quyền truy cập";
        }

        try {
            feedbackService.createVolunteerFeedback(oppId, currentUser.getUserId(), rating, content);
            return "success:Gửi đánh giá thành công!";
        } catch (IllegalStateException | IllegalArgumentException e) {
            return "error:" + e.getMessage();
        } catch (Exception e) {
            return "error:Đã xảy ra lỗi khi gửi đánh giá";
        }
    }

    @PostMapping(value = "/update-feedback", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String updateFeedback(@RequestParam int feedbackId,
            @RequestParam int rating,
            @RequestParam(required = false) String content,
            Authentication authentication) {

        User currentUser = SecurityUtils.getCurrentUser(authentication);
        if (currentUser == null) {
            return "error:Vui lòng đăng nhập";
        }

        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "error:Không có quyền truy cập";
        }

        try {
            feedbackService.updateVolunteerFeedback(feedbackId, rating, content);
            return "success:Cập nhật đánh giá thành công!";
        } catch (IllegalStateException | IllegalArgumentException e) {
            return "error:" + e.getMessage();
        } catch (Exception e) {
            return "error:Đã xảy ra lỗi khi cập nhật đánh giá";
        }
    }

    // My Applications PhiLong
    @GetMapping("/applications")
    public String myApplications(
            Model model,
            Authentication authentication,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "status", defaultValue = "") String filterStatus,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        User currentUser = SecurityUtils.getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName())) {
            return "redirect:/403";
        }

        User freshUser = userService.findByIdWithRole(currentUser.getUserId());
        if (freshUser == null) {
            return "redirect:/login";
        }

        int size = 5;
        Page<Application> paged = applicationService.searchMyApplications(
                currentUser.getUserId(),
                filterStatus,
                q,
                sort,
                page,
                size);

        model.addAttribute("items", paged.getContent());
        model.addAttribute("totalPages", paged.getTotalPages());
        model.addAttribute("currentPage", page);

        // giữ lại các tham số filter/sort để binding ra view
        model.addAttribute("q", q);
        model.addAttribute("filterStatus", filterStatus);
        model.addAttribute("sort", sort);

        model.addAttribute("user", freshUser);
        model.addAttribute("activePage", "applications");

        return "volunteer/my-applications";
    }

    @PostMapping("/applications/cancel")
    public String cancelMyApplication(@RequestParam("appId") Integer appId,
            @RequestParam("cancelReason") String cancelReason,
            Authentication authentication,
            RedirectAttributes ra) {
        User currentUser = SecurityUtils.getCurrentUser(authentication);
        if (currentUser == null)
            return "redirect:/login";
        if (!"VOLUNTEER".equals(currentUser.getRole().getRoleName()))
            return "redirect:/403";

        try {
            applicationService.cancelByVolunteer(appId, currentUser.getUserId(), cancelReason);
            ra.addFlashAttribute("success", "Đã hủy đơn ứng tuyển thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Có lỗi xảy ra khi hủy đơn: " + ex.getMessage());
        }
        return "redirect:/profile/applications";
    }

}
