package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.dto.request.OpportunitySectionForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.SecurityUtils;
import com.fptuni.vms.service.ApplicationService;
import com.fptuni.vms.service.NotificationService;
import com.fptuni.vms.service.OpportunitySectionService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
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

    /** Validate upload ảnh tại controller: chỉ nhận hình ≤ 5MB */
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /** Nhãn tiếng Việt cho enum trạng thái */
    private static Map<String, String> viStatus() {
        return Map.of(
                "OPEN", "Đang mở",
                "CANCELLED", "Đã hủy",
                "CLOSED", "Đã kết thúc"
        );
    }

    @GetMapping
    public String listForOwner(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) Opportunity.OpportunityStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model
    ) {
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            model.addAttribute("error", "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra trạng thái tài khoản  hoặc liên hệ quản trị viên.");
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

    // ====== GET create ======
    @GetMapping("/new")
    public String createForm(Model model) {
        OpportunityForm form = new OpportunityForm();
        var s0 = new OpportunitySectionForm();
        s0.setSectionOrder(1);
        form.getSections().add(s0);

        populateCommon(model, form, "Tạo cơ hội mới");
        return "organization/opportunity-form";
    }

    // ====== GET edit ======
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Opportunity opp = opportunityService.findById(id);
        if (opp == null) {
            return "redirect:/organization/ratings/opportunities";
        }
        OpportunityForm form = mapToForm(opp, sectionService.findByOpportunity(id));
        populateCommon(model, form, "Chỉnh sửa cơ hội");
        return "organization/opportunity-form";
    }

    // ====== POST save (create or update) ======
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OpportunityForm form,
                       BindingResult binding,
                       RedirectAttributes ra,
                       Model model) {

        // VALIDATE thời điểm (phòng trường hợp không dùng @AssertTrue)
        if (form.getStartDate()!=null && form.getStartTime()!=null &&
                form.getEndDate()!=null && form.getEndTime()!=null) {
            var start = LocalDateTime.of(form.getStartDate(), form.getStartTime());
            var end   = LocalDateTime.of(form.getEndDate(), form.getEndTime());
            if (!end.isAfter(start)) {
                binding.rejectValue("endDate","invalid","Ngày/giờ kết thúc phải sau thời điểm bắt đầu");
            }
        }

        // Validate file thumbnail: chỉ cho ảnh và <= 5MB
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            MultipartFile f = form.getThumbnailFile();
            if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType())) {
                binding.rejectValue("thumbnailFile","upload.type","Chỉ chấp nhận tệp hình ảnh (jpg, png, gif, webp)");
            } else if (f.getSize() > MAX_IMAGE_BYTES) {
                binding.rejectValue("thumbnailFile","upload.tooLarge","Ảnh đại diện tối đa 5MB");
            }
        }

        // Validate file trong từng section
        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                MultipartFile f = sf.getImageFile();
                if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType())) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.type", "Ảnh trong phần phải là hình (jpg, png, gif, webp)");
                } else if (f.getSize() > MAX_IMAGE_BYTES) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.tooLarge", "Ảnh trong phần tối đa 5MB");
                }
            }
        }

        if (binding.hasErrors()) {
            model.addAttribute("err", "Dữ liệu chưa hợp lệ, vui lòng kiểm tra các trường bôi đỏ.");
            populateCommon(model, form, form.getOppId()==null?"Tạo cơ hội mới":"Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        // Lấy organization theo user đăng nhập
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            binding.reject("org.missing", "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra trạng thái tài khoản  hoặc liên hệ quản trị viên.");
            model.addAttribute("err", "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra trạng thái tài khoản  hoặc liên hệ quản trị viên.");
            populateCommon(model, form, "Tạo cơ hội");
            return "organization/opportunity-form";
        }

        // Upload thumbnail (nếu có)
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            String url = cloudStorage.uploadFile(form.getThumbnailFile());
            if (url == null) {
                binding.rejectValue("thumbnailFile","upload.fail","Upload ảnh đại diện thất bại");
                model.addAttribute("err", "Upload ảnh thất bại. Vui lòng thử lại.");
                populateCommon(model, form, form.getOppId()==null?"Tạo cơ hội mới":"Chỉnh sửa cơ hội");
                return "organization/opportunity-form";
            }
            form.setThumbnailUrl(url);
        }

        // Lấy bản cũ & chụp snapshot trước khi sửa
        Opportunity old = (form.getOppId() != null) ? opportunityService.findById(form.getOppId()) : null;
        OppSnapshot oldSnap = (old == null) ? null : OppSnapshot.from(old);

        // Map form -> entity
        var start = LocalDateTime.of(form.getStartDate(), form.getStartTime());
        var end   = LocalDateTime.of(form.getEndDate(), form.getEndTime());

        Opportunity opp = (old == null) ? new Opportunity() : old;
        opp.setOrganization(org);
        Category cat = new Category(); cat.setCategoryId(form.getCategoryId());
        opp.setCategory(cat);
        opp.setTitle(form.getTitle());
        opp.setSubtitle(form.getSubtitle());
        opp.setLocation(form.getLocation());
        opp.setNeededVolunteers(form.getNeededVolunteers());
        opp.setStatus(form.getStatus());
        opp.setStartTime(start);
        opp.setEndTime(end);
        if (form.getThumbnailUrl()!=null) {
            opp.setThumbnailUrl(form.getThumbnailUrl());
        }

        // Chuẩn bị Sections
        List<OpportunitySection> toSave = new ArrayList<>();
        int idx = 1;
        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            if (sf.getImageFile()!=null && !sf.getImageFile().isEmpty()) {
                String imgUrl = cloudStorage.uploadFile(sf.getImageFile());
                if (imgUrl == null) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.fail", "Upload ảnh trong phần thất bại");
                } else {
                    sf.setImageUrl(imgUrl);
                }
            }
            OpportunitySection s = new OpportunitySection();
            s.setOpportunity(opp);
            s.setSectionOrder(sf.getSectionOrder()!=null?sf.getSectionOrder():idx);
            s.setHeading(sf.getHeading());
            s.setContent(sf.getContent());
            s.setImageUrl(sf.getImageUrl());
            s.setCaption(sf.getCaption());
            toSave.add(s);
            idx++;
        }

        if (binding.hasErrors()) {
            model.addAttribute("err", "Upload ảnh phần nội dung thất bại. Vui lòng kiểm tra lại.");
            populateCommon(model, form, form.getOppId()==null?"Tạo cơ hội mới":"Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        // Lưu DB (bắt các vi phạm ràng buộc như FK category…)
        try {
            opp = opportunityService.save(opp);
            sectionService.replaceSections(opp, toSave);
        } catch (DataIntegrityViolationException | PersistenceException ex) {
            binding.reject("db.constraint",
                    "Lưu thất bại do vi phạm ràng buộc dữ liệu (ví dụ: danh mục không tồn tại, dữ liệu vượt giới hạn).");
            model.addAttribute("err", "Lưu thất bại do vi phạm ràng buộc dữ liệu.");
            populateCommon(model, form, form.getOppId()==null?"Tạo cơ hội mới":"Chỉnh sửa cơ hội");
            return "organization/opportunity-form";
        }

        // ===== Notifications + Email =====
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
            if (!msg.isBlank()) {
                notificationService.notifyUsers(recipients, title, msg, "INFO", publicLink, me.getUserId(), org.getOrgId());
            }
            ra.addFlashAttribute("ok", "Cập nhật cơ hội thành công.");
        }

        return "redirect:/org/opps/%d/edit".formatted(opp.getOppId());
    }

    // ====== POST cancel (đổi sang CANCELLED) ======
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

        // Gửi thông báo hủy
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        Integer orgId = (org != null) ? org.getOrgId() : null;

        String title = "Thông báo hủy: " + opp.getTitle();
        String msg = "Cơ hội \"" + opp.getTitle() + "\" đã được hủy. Rất mong bạn thông cảm.";
        String link = "/opportunities/" + opp.getOppId();

        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());
        notificationService.notifyUsers(recipients, title, msg, "ALERT", link, me.getUserId(), orgId);

        ra.addFlashAttribute("ok", "Đã hủy sự kiện và gửi thông báo.");
        return "redirect:/org/opps";

    }

    // ===== helpers =====

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
        int i=1;
        for (OpportunitySection s : sections) {
            OpportunitySectionForm sf = new OpportunitySectionForm();
            sf.setSectionOrder(s.getSectionOrder()!=null?s.getSectionOrder():i);
            sf.setHeading(s.getHeading());
            sf.setContent(s.getContent());
            sf.setImageUrl(s.getImageUrl());
            sf.setCaption(s.getCaption());
            sfs.add(sf);
            i++;
        }
        if (sfs.isEmpty()) {
            var one = new OpportunitySectionForm(); one.setSectionOrder(1); sfs.add(one);
        }
        f.setSections(sfs);
        return f;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String buildCreateMessage(Opportunity opp, Organization org) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tổ chức ").append(org.getName()).append(" đã tạo cơ hội mới:\n");
        sb.append("• Tiêu đề: ").append(opp.getTitle()).append("\n");
        if (opp.getSubtitle()!=null && !opp.getSubtitle().isBlank())
            sb.append("• Mô tả: ").append(opp.getSubtitle()).append("\n");
        if (opp.getLocation()!=null && !opp.getLocation().isBlank())
            sb.append("• Địa điểm: ").append(opp.getLocation()).append("\n");
        sb.append("• Thời gian: ")
                .append(FMT.format(opp.getStartTime()))
                .append(" → ")
                .append(FMT.format(opp.getEndTime()))
                .append("\n");
        sb.append("• Trạng thái: ").append(viStatus().getOrDefault(opp.getStatus().name(), opp.getStatus().name()));
        return sb.toString();
    }

    // Snapshot bất biến để so sánh thay đổi
    private record OppSnapshot(
            String title,
            String subtitle,
            String location,
            Integer neededVolunteers,
            Opportunity.OpportunityStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        static OppSnapshot from(Opportunity o) {
            return new OppSnapshot(
                    o.getTitle(),
                    o.getSubtitle(),
                    o.getLocation(),
                    o.getNeededVolunteers(),
                    o.getStatus(),
                    o.getStartTime(),
                    o.getEndTime()
            );
        }
    }

    private String buildUpdateMessage(OppSnapshot oldO, Opportunity newO, Organization org) {
        List<String> changes = new ArrayList<>();

        if (!Objects.equals(oldO.title(), newO.getTitle()))
            changes.add("• Tiêu đề: \"" + nullSafe(oldO.title()) + "\" → \"" + nullSafe(newO.getTitle()) + "\"");

        if (!Objects.equals(oldO.subtitle(), newO.getSubtitle()))
            changes.add("• Mô tả ngắn: \"" + nullSafe(oldO.subtitle()) + "\" → \"" + nullSafe(newO.getSubtitle()) + "\"");

        if (!Objects.equals(oldO.location(), newO.getLocation()))
            changes.add("• Địa điểm: \"" + nullSafe(oldO.location()) + "\" → \"" + nullSafe(newO.getLocation()) + "\"");

        if (!Objects.equals(oldO.neededVolunteers(), newO.getNeededVolunteers()))
            changes.add("• Số TNV cần: " + nullSafe(oldO.neededVolunteers()) + " → " + nullSafe(newO.getNeededVolunteers()));

        if (!Objects.equals(oldO.startTime(), newO.getStartTime()) || !Objects.equals(oldO.endTime(), newO.getEndTime()))
            changes.add("• Thời gian: " + FMT.format(oldO.startTime()) + " → " + FMT.format(newO.getStartTime())
                    + " | " + FMT.format(oldO.endTime()) + " → " + FMT.format(newO.getEndTime()));

        if (!Objects.equals(oldO.status(), newO.getStatus()))
            changes.add("• Trạng thái: " + viStatus().getOrDefault(oldO.status().name(), oldO.status().name())
                    + " → " + viStatus().getOrDefault(newO.getStatus().name(), newO.getStatus().name()));

        if (changes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Tổ chức ").append(org.getName()).append(" đã cập nhật cơ hội:\n");
        for (String c : changes) sb.append(c).append("\n");
        return sb.toString().trim();
    }

    private String nullSafe(Object o) { return o == null ? "(trống)" : o.toString(); }
}
