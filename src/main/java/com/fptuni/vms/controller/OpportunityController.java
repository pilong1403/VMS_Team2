package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.dto.request.OpportunitySectionForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.*;
import com.fptuni.vms.security.SecurityUtils;
import com.fptuni.vms.service.*;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/org/opps")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final OpportunitySectionService sectionService;
    private final CloudStorageService cloudStorage;
    private final OrganizationService organizationService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    public OpportunityController(OpportunityService opportunityService,
            OpportunitySectionService sectionService,
            CloudStorageService cloudStorage,
            OrganizationService organizationService,
            ApplicationService applicationService,
            NotificationService notificationService) {
        this.opportunityService = opportunityService;
        this.sectionService = sectionService;
        this.cloudStorage = cloudStorage;
        this.organizationService = organizationService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
    }

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private static Map<String, String> viStatus() {
        return Map.of(
                "OPEN", "Đang mở",
                "CANCELLED", "Đã hủy",
                "CLOSED", "Đã kết thúc");
    }

    @GetMapping
    public String listForOwner(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) Opportunity.OpportunityStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            model.addAttribute("error",
                    "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra hoặc liên hệ quản trị viên.");
            model.addAttribute("page", Page.empty());
            model.addAttribute("statusVN", viStatus());
            return "organization/opportunity-list";
        }

        var result = opportunityService.searchByOrg(org.getOrgId(), q, status, page, size);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statusVN", viStatus());
        return "organization/opportunity-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OpportunityForm form = new OpportunityForm();
        var s0 = new OpportunitySectionForm();
        s0.setSectionOrder(1);
        form.getSections().add(s0);
        form.setStatus(Opportunity.OpportunityStatus.OPEN);
        populateCommon(model, form, "Tạo cơ hội mới");
        return "organization/opportunity-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Opportunity opp = opportunityService.findById(id);
        if (opp == null)
            return "redirect:/org/opps";
        OpportunityForm form = mapToForm(opp, sectionService.findByOpportunity(id));
        populateCommon(model, form, "Chỉnh sửa cơ hội");
        return "organization/opportunity-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OpportunityForm form,
            BindingResult binding,
            RedirectAttributes ra,
            Model model) {

        if (form.getStartDate() != null && form.getStartTime() != null &&
                form.getEndDate() != null && form.getEndTime() != null) {
            var start = LocalDateTime.of(form.getStartDate(), form.getStartTime());
            var end = LocalDateTime.of(form.getEndDate(), form.getEndTime());
            if (!end.isAfter(start))
                binding.rejectValue("endDate", "invalid", "Ngày/giờ kết thúc phải sau thời điểm bắt đầu");
        }

        // Validate file thumbnail
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            MultipartFile f = form.getThumbnailFile();
            if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType()))
                binding.rejectValue("thumbnailFile", "upload.type", "Chỉ chấp nhận tệp hình ảnh (jpg, png, gif, webp)");
            else if (f.getSize() > MAX_IMAGE_BYTES)
                binding.rejectValue("thumbnailFile", "upload.tooLarge", "Ảnh đại diện tối đa 5MB");
        }

        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                MultipartFile f = sf.getImageFile();
                if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType()))
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.type",
                            "Ảnh trong phần phải là hình (jpg, png, gif, webp)");
                else if (f.getSize() > MAX_IMAGE_BYTES)
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.tooLarge",
                            "Ảnh trong phần tối đa 5MB");
            }
        }

        if (binding.hasErrors()) {
            model.addAttribute("err", "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.");
            populateCommon(model, form, form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            binding.reject("org.missing", "Không tìm thấy thông tin tổ chức hợp lệ.");
            model.addAttribute("err", "Không tìm thấy thông tin tổ chức hợp lệ.");
            populateCommon(model, form, "Tạo cơ hội");
            return "organization/opportunity-form";
        }

        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            String url = cloudStorage.uploadFile(form.getThumbnailFile());
            if (url == null) {
                binding.rejectValue("thumbnailFile", "upload.fail", "Upload ảnh thất bại");
                model.addAttribute("err", "Upload ảnh thất bại. Vui lòng thử lại.");
                populateCommon(model, form, form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội");
                return "organization/opportunity-form";
            }
            form.setThumbnailUrl(url);
        }

        Opportunity old = (form.getOppId() != null) ? opportunityService.findById(form.getOppId()) : null;
        OppSnapshot oldSnap = (old == null) ? null : OppSnapshot.from(old);

        var start = LocalDateTime.of(form.getStartDate(), form.getStartTime());
        var end = LocalDateTime.of(form.getEndDate(), form.getEndTime());

        Opportunity opp = (old == null) ? new Opportunity() : old;
        opp.setOrganization(org);
        Category cat = new Category();
        cat.setCategoryId(form.getCategoryId());
        opp.setCategory(cat);
        opp.setTitle(form.getTitle());
        opp.setSubtitle(form.getSubtitle());
        opp.setLocation(form.getLocation());
        opp.setNeededVolunteers(form.getNeededVolunteers());
        opp.setStatus(form.getStatus());
        opp.setStartTime(start);
        opp.setEndTime(end);
        if (form.getThumbnailUrl() != null)
            opp.setThumbnailUrl(form.getThumbnailUrl());

        List<OpportunitySection> toSave = new ArrayList<>();
        int idx = 1;
        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                String imgUrl = cloudStorage.uploadFile(sf.getImageFile());
                if (imgUrl == null)
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.fail", "Upload ảnh thất bại");
                else
                    sf.setImageUrl(imgUrl);
            }
            OpportunitySection s = new OpportunitySection();
            s.setOpportunity(opp);
            s.setSectionOrder(sf.getSectionOrder() != null ? sf.getSectionOrder() : idx);
            s.setHeading(sf.getHeading());
            s.setContent(sf.getContent());
            s.setImageUrl(sf.getImageUrl());
            s.setCaption(sf.getCaption());
            toSave.add(s);
            idx++;
        }

        if (binding.hasErrors()) {
            model.addAttribute("err", "Upload ảnh phần nội dung thất bại.");
            populateCommon(model, form, form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        try {
            opp = opportunityService.save(opp);
            sectionService.replaceSections(opp, toSave);
        } catch (DataIntegrityViolationException | PersistenceException ex) {
            binding.reject("db.constraint", "Lưu thất bại do vi phạm ràng buộc dữ liệu.");
            model.addAttribute("err", "Lưu thất bại do vi phạm dữ liệu.");
            populateCommon(model, form, form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        String publicLink = "/opportunities/" + opp.getOppId();
        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());

        if (oldSnap == null) {
            String title = "Cơ hội mới: " + opp.getTitle();
            String msg = buildCreateMessage(opp, org);
            notificationService.notifyUsers(recipients, title, msg, "INFO", publicLink, me.getUserId(), org.getOrgId());
            ra.addFlashAttribute("ok", "Tạo cơ hội thành công.");
        } else {
            String title = "Cập nhật cơ hội: " + opp.getTitle();
            String msg = buildUpdateMessage(oldSnap, opp, org);
            if (!msg.isBlank())
                notificationService.notifyUsers(recipients, title, msg, "INFO", publicLink, me.getUserId(),
                        org.getOrgId());
            ra.addFlashAttribute("ok", "Cập nhật cơ hội thành công.");
        }

        return "redirect:/org/opps/%d/edit".formatted(opp.getOppId());
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id, RedirectAttributes ra) {
        Opportunity opp = opportunityService.findById(id);
        if (opp == null) {
            ra.addFlashAttribute("err", "Không tìm thấy sự kiện.");
            return "redirect:/org/opps";
        }
        if (opp.getEndTime() != null && !LocalDateTime.now().isBefore(opp.getEndTime())) {
            ra.addFlashAttribute("err", "Sự kiện đã kết thúc, không thể hủy.");
            return "redirect:/org/opps";
        }

        opp.setStatus(Opportunity.OpportunityStatus.CANCELLED);
        opportunityService.save(opp);

        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        Integer orgId = (org != null) ? org.getOrgId() : null;

        String title = "Thông báo hủy: " + opp.getTitle();
        String msg = "Cơ hội \"" + opp.getTitle() + "\" đã bị hủy bởi tổ chức "
                + (org != null ? org.getName() : "") + ". Rất mong bạn thông cảm.";
        String link = "/opportunities/" + opp.getOppId();

        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());
        notificationService.notifyUsers(recipients, title, msg, "ALERT", link, me.getUserId(), orgId);

        ra.addFlashAttribute("ok", "Đã hủy sự kiện và gửi thông báo đến tình nguyện viên.");
        return "redirect:/org/opps";
    }

    private void populateCommon(Model model, OpportunityForm form, String title) {
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", title);
        model.addAttribute("statuses", Opportunity.OpportunityStatus.values());
        model.addAttribute("statusVN", viStatus());
        model.addAttribute("categories", opportunityService.getCategoriesWithOpportunities());
    }

    private OpportunityForm mapToForm(Opportunity o, List<OpportunitySection> sections) {
        OpportunityForm f = new OpportunityForm();
        f.setOppId(o.getOppId());
        f.setCategoryId(o.getCategory().getCategoryId());
        f.setTitle(o.getTitle());
        f.setSubtitle(o.getSubtitle());
        f.setLocation(o.getLocation());
        f.setNeededVolunteers(o.getNeededVolunteers());
        f.setStatus(o.getStatus());
        f.setStartDate(o.getStartTime().toLocalDate());
        f.setStartTime(o.getStartTime().toLocalTime());
        f.setEndDate(o.getEndTime().toLocalDate());
        f.setEndTime(o.getEndTime().toLocalTime());
        f.setThumbnailUrl(o.getThumbnailUrl());

        List<OpportunitySectionForm> sfs = new ArrayList<>();
        int i = 1;
        for (OpportunitySection s : sections) {
            OpportunitySectionForm sf = new OpportunitySectionForm();
            sf.setSectionOrder(s.getSectionOrder() != null ? s.getSectionOrder() : i);
            sf.setHeading(s.getHeading());
            sf.setContent(s.getContent());
            sf.setImageUrl(s.getImageUrl());
            sf.setCaption(s.getCaption());
            sfs.add(sf);
            i++;
        }
        if (sfs.isEmpty()) {
            var one = new OpportunitySectionForm();
            one.setSectionOrder(1);
            sfs.add(one);
        }
        f.setSections(sfs);
        return f;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String buildCreateMessage(Opportunity opp, Organization org) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tổ chức ").append(org.getName()).append(" đã tạo cơ hội mới:\n")
                .append("• Tiêu đề: ").append(opp.getTitle()).append("\n");
        if (opp.getSubtitle() != null && !opp.getSubtitle().isBlank())
            sb.append("• Mô tả: ").append(opp.getSubtitle()).append("\n");
        if (opp.getLocation() != null && !opp.getLocation().isBlank())
            sb.append("• Địa điểm: ").append(opp.getLocation()).append("\n");
        sb.append("• Thời gian: ").append(FMT.format(opp.getStartTime())).append(" → ")
                .append(FMT.format(opp.getEndTime())).append("\n")
                .append("• Trạng thái: ")
                .append(viStatus().getOrDefault(opp.getStatus().name(), opp.getStatus().name()));
        return sb.toString();
    }

    private record OppSnapshot(String title, String subtitle, String location,
            Integer neededVolunteers, Opportunity.OpportunityStatus status,
            LocalDateTime startTime, LocalDateTime endTime) {
        static OppSnapshot from(Opportunity o) {
            return new OppSnapshot(o.getTitle(), o.getSubtitle(), o.getLocation(),
                    o.getNeededVolunteers(), o.getStatus(), o.getStartTime(), o.getEndTime());
        }
    }

    private String buildUpdateMessage(OppSnapshot old, Opportunity o, Organization org) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(old.title, o.getTitle()))
            changes.add("Tiêu đề");
        if (!Objects.equals(old.subtitle, o.getSubtitle()))
            changes.add("Mô tả");
        if (!Objects.equals(old.location, o.getLocation()))
            changes.add("Địa điểm");
        if (!Objects.equals(old.neededVolunteers, o.getNeededVolunteers()))
            changes.add("Số lượng TNV");
        if (!Objects.equals(old.status, o.getStatus()))
            changes.add("Trạng thái");
        if (!Objects.equals(old.startTime, o.getStartTime()) || !Objects.equals(old.endTime, o.getEndTime()))
            changes.add("Thời gian");

        if (changes.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Cơ hội \"").append(o.getTitle()).append("\" của tổ chức ").append(org.getName())
                .append(" đã được cập nhật (").append(String.join(", ", changes)).append("):\n")
                .append("• Từ: ").append(FMT.format(o.getStartTime())).append(" → ").append(FMT.format(o.getEndTime()))
                .append("\n")
                .append("• Trạng thái: ").append(viStatus().getOrDefault(o.getStatus().name(), o.getStatus().name()));
        return sb.toString();
    }
}
