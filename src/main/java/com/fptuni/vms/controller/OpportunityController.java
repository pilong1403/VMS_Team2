package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.CategoryService;
import com.fptuni.vms.service.OpportunitySectionService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.repository.OrganizationRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/opportunity")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final OpportunitySectionService sectionService;
    private final OrganizationRepository organizationRepository;
    private final CategoryService categoryService;
    private final CloudStorageService cloudStorageService;

    public OpportunityController(OpportunityService opportunityService,
                                 OpportunitySectionService sectionService,
                                 OrganizationRepository organizationRepository,
                                 CategoryService categoryService,
                                 CloudStorageService cloudStorageService) {
        this.opportunityService = opportunityService;
        this.sectionService = sectionService;
        this.organizationRepository = organizationRepository;
        this.categoryService = categoryService;
        this.cloudStorageService = cloudStorageService;
    }

    /* ---- (1) Tránh 405 khi ai đó GET /opportunity: chuyển về danh sách ---- */
    @GetMapping
    public String rootGetRedirect() {
        return "redirect:/opportunity/org";
    }

    /* ========== LIST (thuộc tổ chức hiện tại) ========== */
    @GetMapping("/org")
    public String listForOrg(Model model) {
        Integer ownerId = getCurrentUserId();
        Organization org = organizationRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa có tổ chức được duyệt"));
        model.addAttribute("org", org);
        model.addAttribute("activePage", "opp_list");
        model.addAttribute("opportunities", opportunityService.findByOrganization(org.getOrgId()));
        return "organization/opportunity-list";
    }

    /* ---- (2) GET trang tạo mới: BẮT BUỘC phải có để hiển thị form ---- */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Organization org = getCurrentOrg();

        OpportunityForm form = new OpportunityForm();
        form.setStatus(Opportunity.OpportunityStatus.OPEN);
        LocalDateTime base = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        form.setStartTime(base.withMinute(0));
        form.setEndTime(base.plusHours(2));

        model.addAttribute("org", org);
        model.addAttribute("form", form);
        model.addAttribute("activePage", "opp_create");
        model.addAttribute("categories", categoryService.listAll());
        return "organization/opportunity-form";
    }

    /* ========== CREATE (POST /opportunity) ========== */
    @PostMapping
    @Transactional
    public String create(@Valid @ModelAttribute("form") OpportunityForm form,
                         BindingResult binding,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                         @RequestParam(value = "sectionFiles", required = false) List<MultipartFile> sectionFiles,
                         Model model) {

        Organization org = getCurrentOrg();

        if (!form.isTimeValid()) {
            binding.rejectValue("endTime", "time.invalid", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (binding.hasErrors()) {
            model.addAttribute("org", org);
            model.addAttribute("categories", categoryService.listAll());
            return "organization/opportunity-form";
        }

        Opportunity o = mapToEntity(new Opportunity(), org, form);

        // upload thumbnail nếu có
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String url = cloudStorageService.uploadFile(thumbnailFile);
            if (url != null) o.setThumbnailUrl(url);
        }

        Opportunity persisted = opportunityService.save(o);

        // upload ảnh từng section (nếu có)
        if (sectionFiles != null && !sectionFiles.isEmpty()) {
            for (int i = 0; i < form.getSections().size(); i++) {
                var sectionForm = form.getSections().get(i);
                if (i < sectionFiles.size()) {
                    MultipartFile file = sectionFiles.get(i);
                    if (file != null && !file.isEmpty()) {
                        String url = cloudStorageService.uploadFile(file);
                        sectionForm.setImageUrl(url);
                    }
                }
            }
        }

        sectionService.replaceForOpportunity(persisted, form.getSections());
        return "redirect:/opportunity/org";
    }

    /* ========== EDIT (GET /opportunity/{id}/edit) ========== */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Organization org = getCurrentOrg();
        Opportunity o = mustOwn(id, org);
        OpportunityForm form = toForm(o);

        // nạp sections hiện có
        form.setSections(sectionService.listByOpportunity(id)
                .stream()
                .map(s -> {
                    OpportunityForm.SectionForm sf = new OpportunityForm.SectionForm();
                    sf.setSectionId(s.getSectionId());
                    sf.setSectionOrder(s.getSectionOrder());
                    sf.setHeading(s.getHeading());
                    sf.setContent(s.getContent());
                    sf.setImageUrl(s.getImageUrl());
                    sf.setCaption(s.getCaption());
                    return sf;
                })
                .toList()
        );

        model.addAttribute("org", org);
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.listAll());
        return "organization/opportunity-form";
    }

    /* ========== UPDATE (POST /opportunity/{id}) ========== */
    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("form") OpportunityForm form,
                         BindingResult binding,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                         @RequestParam(value = "sectionFiles", required = false) List<MultipartFile> sectionFiles,
                         Model model) {

        Organization org = getCurrentOrg();
        Opportunity existing = mustOwn(id, org);

        if (existing.getStartTime() != null && existing.getStartTime().isBefore(LocalDateTime.now())) {
            binding.reject("locked", "Sự kiện đã bắt đầu, không thể sửa các trường chính.");
        }
        if (!form.isTimeValid()) {
            binding.rejectValue("endTime", "time.invalid", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (binding.hasErrors()) {
            model.addAttribute("org", org);
            model.addAttribute("categories", categoryService.listAll());
            // giữ lại sections hiện có để render lại form
            form.setSections(sectionService.listByOpportunity(id).stream().map(s -> {
                OpportunityForm.SectionForm sf = new OpportunityForm.SectionForm();
                sf.setSectionId(s.getSectionId());
                sf.setSectionOrder(s.getSectionOrder());
                sf.setHeading(s.getHeading());
                sf.setContent(s.getContent());
                sf.setImageUrl(s.getImageUrl());
                sf.setCaption(s.getCaption());
                return sf;
            }).toList());
            return "organization/opportunity-form";
        }

        existing = mapToEntity(existing, org, form);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String url = cloudStorageService.uploadFile(thumbnailFile);
            if (url != null) existing.setThumbnailUrl(url);
        }

        // upload ảnh section mới
        if (sectionFiles != null && !sectionFiles.isEmpty()) {
            for (int i = 0; i < form.getSections().size(); i++) {
                var sectionForm = form.getSections().get(i);
                if (i < sectionFiles.size()) {
                    MultipartFile file = sectionFiles.get(i);
                    if (file != null && !file.isEmpty()) {
                        String url = cloudStorageService.uploadFile(file);
                        sectionForm.setImageUrl(url);
                    }
                }
            }
        }

        Opportunity merged = opportunityService.save(existing);
        sectionService.replaceForOpportunity(merged, form.getSections());
        return "redirect:/opportunity/org";
    }

    /* ========== DELETE (soft) ========== */
    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Integer id) {
        Organization org = getCurrentOrg();
        mustOwn(id, org);
        opportunityService.delete(id); // set CANCELLED
        return "redirect:/opportunity/org";
    }

    /* ========== helpers ========== */
    private Integer getCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof com.fptuni.vms.security.CustomUserDetails u) {
            User domain = u.getUser();
            return domain.getUserId();
        }
        throw new IllegalStateException("Bạn chưa đăng nhập.");
    }

    private Organization getCurrentOrg() {
        return organizationRepository.findByOwnerId(getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Bạn chưa có tổ chức được duyệt"));
    }

    private Opportunity mustOwn(Integer oppId, Organization org) {
        Opportunity o = opportunityService.findById(oppId);
        if (o == null || !o.getOrganization().getOrgId().equals(org.getOrgId())) {
            throw new IllegalStateException("Không tìm thấy cơ hội của tổ chức bạn");
        }
        return o;
    }

    private Opportunity mapToEntity(Opportunity o, Organization org, OpportunityForm f) {
        o.setOrganization(org);

        Category c = new Category();
        c.setCategoryId(f.getCategoryId());
        o.setCategory(c);

        o.setTitle(f.getTitle());
        o.setSubtitle(f.getSubtitle());
        o.setLocation(f.getLocation());
        o.setThumbnailUrl(f.getThumbnailUrl());
        o.setNeededVolunteers(f.getNeededVolunteers());
        o.setStatus(f.getStatus());
        o.setStartTime(f.getStartTime());
        o.setEndTime(f.getEndTime());
        return o;
    }

    private OpportunityForm toForm(Opportunity o) {
        OpportunityForm f = new OpportunityForm();
        f.setOppId(o.getOppId());
        f.setCategoryId(o.getCategory().getCategoryId());
        f.setTitle(o.getTitle());
        f.setSubtitle(o.getSubtitle());
        f.setLocation(o.getLocation());
        f.setThumbnailUrl(o.getThumbnailUrl());
        f.setNeededVolunteers(o.getNeededVolunteers());
        f.setStatus(o.getStatus());
        f.setStartTime(o.getStartTime());
        f.setEndTime(o.getEndTime());
        return f;
    }
}
