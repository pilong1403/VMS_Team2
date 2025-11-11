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
                "DRAFT", "Bản nháp",
                "OPEN", "Đang mở",
                "CANCELLED", "Đã hủy",
                "CLOSED", "Đã kết thúc"
        );
    }

    /** Trạng thái hợp lệ theo trạng thái hiện tại (basis). */
    private List<Opportunity.OpportunityStatus> allowedStatusesFor(Opportunity.OpportunityStatus current) {
        if (current == null) {
            return List.of(Opportunity.OpportunityStatus.DRAFT, Opportunity.OpportunityStatus.OPEN);
        }
        return switch (current) {
            case DRAFT -> List.of(Opportunity.OpportunityStatus.DRAFT, Opportunity.OpportunityStatus.OPEN);
            case OPEN -> List.of(Opportunity.OpportunityStatus.OPEN, Opportunity.OpportunityStatus.CANCELLED);
            case CANCELLED -> List.of(Opportunity.OpportunityStatus.CANCELLED);
            case CLOSED -> List.of(Opportunity.OpportunityStatus.CLOSED);
        };
    }

    @GetMapping
    public String listForOwner(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "status", required = false) Opportunity.OpportunityStatus status,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @RequestParam(value = "timeOrder", required = false) String timeOrder,
                               Model model) {
        model.addAttribute("activePage", "OppManagement");

        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            model.addAttribute("error", "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra hoặc liên hệ quản trị viên.");
            model.addAttribute("page", Page.empty());
            model.addAttribute("statusVN", viStatus());
            model.addAttribute("q", q);
            model.addAttribute("status", status);
            model.addAttribute("timeOrder", timeOrder);
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 1);
            model.addAttribute("num", size);
            return "organization/opportunity-list";
        }

        int zeroBased = Math.max(page - 1, 0);
        Page<Opportunity> result = opportunityService.searchByOrg(
                org.getOrgId(), q, status, zeroBased, size, timeOrder
        );

        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statusVN", viStatus());
        model.addAttribute("timeOrder", timeOrder);

        int currentPage = result.getNumber() + 1;
        int totalPages = result.getTotalPages() == 0 ? 1 : result.getTotalPages();
        int window = 5;
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + window - 1);
        if (endPage - startPage + 1 < window) {
            startPage = Math.max(1, endPage - window + 1);
        }

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("num", size);

        return "organization/opportunity-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OpportunityForm form = new OpportunityForm();
        var s0 = new OpportunitySectionForm();
        s0.setSectionOrder(1);
        form.getSections().add(s0);
        form.setStatus(Opportunity.OpportunityStatus.DRAFT);

        populateCommon(model, form, "Tạo cơ hội mới", Opportunity.OpportunityStatus.DRAFT, false);
        model.addAttribute("initialStatus", Opportunity.OpportunityStatus.DRAFT.name());
        return "organization/opportunity-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Opportunity opp = opportunityService.findById(id);
        if (opp == null) return "redirect:/org/opps";

        boolean startedLock = opp.getStartTime() != null && !LocalDateTime.now().isBefore(opp.getStartTime());
        boolean cancelledLock = opp.getStatus() == Opportunity.OpportunityStatus.CANCELLED;
        boolean closedLock = opp.getStatus() == Opportunity.OpportunityStatus.CLOSED;
        boolean locked = startedLock || cancelledLock || closedLock;

        OpportunityForm form = mapToForm(opp, sectionService.findByOpportunity(id));

        populateCommon(model, form,
                locked ? "Chi tiết cơ hội (đã khóa)" : "Chỉnh sửa cơ hội",
                opp.getStatus(),
                startedLock);

        model.addAttribute("initialStatus", opp.getStatus().name());
        if (locked) {
            model.addAttribute("err", startedLock
                    ? "Sự kiện đã bắt đầu, không thể chỉnh sửa."
                    : (cancelledLock ? "Sự kiện đã hủy, không thể chỉnh sửa."
                    : "Sự kiện đã kết thúc, không thể chỉnh sửa."));
        }
        return "organization/opportunity-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OpportunityForm form,
                       BindingResult binding,
                       RedirectAttributes ra,
                       Model model,
                       @RequestParam(value = "confirmPublish", defaultValue = "false") String confirmPublish) {

        // === Nạp opp cũ để so sánh (đổi giờ hay không) ===
        Opportunity old = (form.getOppId() != null) ? opportunityService.findById(form.getOppId()) : null;

        // === Ghép thời gian từ form ===
        LocalDateTime start = null, end = null;
        if (form.getStartDate() != null && form.getStartTime() != null) {
            start = LocalDateTime.of(form.getStartDate(), form.getStartTime());
        }
        if (form.getEndDate() != null && form.getEndTime() != null) {
            end = LocalDateTime.of(form.getEndDate(), form.getEndTime());
        }

        // === “end > start” (phụ trợ cho Bean Validation) ===
        if (start != null && end != null && !end.isAfter(start)) {
            binding.rejectValue("endDate", "invalid", "Ngày/giờ kết thúc phải sau thời điểm bắt đầu");
        }

        // === 24h rule: chỉ khi tạo mới hoặc có đổi giờ bắt đầu ===
        if (start != null) {
            boolean startChanged = (old == null) || !start.equals(old.getStartTime());
            if (startChanged) {
                LocalDateTime minStart = LocalDateTime.now().plusHours(24);
                if (start.isBefore(minStart)) {
                    binding.rejectValue("startDate", "invalid.soon",
                            "Thời điểm bắt đầu phải sau ít nhất 24 giờ kể từ hiện tại");
                }
            }
        }

        // === Validate file thumbnail ===
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            MultipartFile f = form.getThumbnailFile();
            if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType())) {
                binding.rejectValue("thumbnailFile", "upload.type",
                        "Chỉ chấp nhận tệp hình ảnh (jpg, png, gif, webp)");
            } else if (f.getSize() > MAX_IMAGE_BYTES) {
                binding.rejectValue("thumbnailFile", "upload.tooLarge", "Ảnh đại diện tối đa 5MB");
            }
        }

        // === Validate ảnh từng section ===
        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                MultipartFile f = sf.getImageFile();
                if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType())) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.type",
                            "Ảnh trong phần phải là hình (jpg, png, gif, webp)");
                } else if (f.getSize() > MAX_IMAGE_BYTES) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.tooLarge",
                            "Ảnh trong phần tối đa 5MB");
                }
            }
        }

        // === Validate thứ tự section (server) ===
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            Integer ord = sf.getSectionOrder();
            if (ord == null || ord < 1) {
                binding.rejectValue("sections[" + i + "].sectionOrder", "order.invalid", "Thứ tự phải là số dương (>=1)");
                continue;
            }
            if (!seen.add(ord)) {
                binding.rejectValue("sections[" + i + "].sectionOrder", "order.dup", "Thứ tự bị trùng. Vui lòng chọn số khác.");
            }
        }

        // === Kiểm tra tổ chức ===
        User me = SecurityUtils.getCurrentUser();
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            binding.reject("org.missing", "Không tìm thấy thông tin tổ chức hợp lệ.");
        }

        // === Nếu có lỗi đến đây: render lại form ===
        if (binding.hasErrors()) {
            Opportunity.OpportunityStatus basis = (form.getOppId() == null)
                    ? Opportunity.OpportunityStatus.DRAFT
                    : Optional.ofNullable(old).map(Opportunity::getStatus)
                    .orElse(Opportunity.OpportunityStatus.DRAFT);

            model.addAttribute("err", "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.");
            model.addAttribute("initialStatus", basis.name());

            boolean startedLock = false;
            if (old != null && old.getStartTime() != null) {
                startedLock = !LocalDateTime.now().isBefore(old.getStartTime());
            }

            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    basis, startedLock);
            return "organization/opportunity-form";
        }

        // === Chặn lưu nếu đã bắt đầu/hủy/kết thúc ===
        if (old != null) {
            boolean startedLock = old.getStartTime() != null && !LocalDateTime.now().isBefore(old.getStartTime());
            if (startedLock || old.getStatus() == Opportunity.OpportunityStatus.CANCELLED
                    || old.getStatus() == Opportunity.OpportunityStatus.CLOSED) {
                model.addAttribute("err", startedLock
                        ? "Sự kiện đã bắt đầu, không thể lưu chỉnh sửa."
                        : (old.getStatus() == Opportunity.OpportunityStatus.CANCELLED
                        ? "Sự kiện đã hủy, không thể lưu chỉnh sửa."
                        : "Sự kiện đã kết thúc, không thể lưu chỉnh sửa."));
                form.setStatus(old.getStatus());
                model.addAttribute("initialStatus", old.getStatus().name());
                populateCommon(model, form, "Chi tiết cơ hội (đã khóa)", old.getStatus(), startedLock);
                return "organization/opportunity-form";
            }
        }

        // === Ràng buộc trạng thái theo basis ===
        Opportunity.OpportunityStatus oldStatus = (old == null)
                ? Opportunity.OpportunityStatus.DRAFT
                : old.getStatus();

        var allowed = allowedStatusesFor(oldStatus);
        var requested = form.getStatus();
        if (requested == null || !allowed.contains(requested)) {
            String msg = switch (oldStatus) {
                case DRAFT -> "Bản nháp chỉ có thể giữ DRAFT hoặc chuyển sang OPEN.";
                case OPEN -> "Chỉ được chọn OPEN hoặc CANCELLED (không thể quay về DRAFT).";
                case CANCELLED -> "Sự kiện đã HỦY, không thể đổi trạng thái.";
                case CLOSED -> "Sự kiện CLOSED đã bị khóa, không thể chỉnh sửa.";
            };
            model.addAttribute("err", msg);
            form.setStatus(oldStatus);
            model.addAttribute("initialStatus", oldStatus.name());
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, false);
            return "organization/opportunity-form";
        }

        // === (start, end) đã có ở trên, dùng kiểm tra overlap ===
        List<Opportunity> overlaps = opportunityService.findOverlapsForOrg(
                org.getOrgId(),
                form.getOppId(),
                start, end,
                5
        );
        if (!overlaps.isEmpty()) {
            StringBuilder detail = new StringBuilder("Thời gian bị chồng lấn với các cơ hội sau:\n");
            for (Opportunity o : overlaps) {
                detail.append("• ")
                        .append(o.getTitle())
                        .append(" (")
                        .append(FMT.format(o.getStartTime()))
                        .append(" → ")
                        .append(FMT.format(o.getEndTime()))
                        .append(")\n");
            }
            binding.rejectValue("startDate", "time.overlap", "Khoảng thời gian trùng với cơ hội khác.");
            binding.rejectValue("endDate", "time.overlap", "Khoảng thời gian trùng với cơ hội khác.");

            model.addAttribute("err", detail.toString().trim());
            model.addAttribute("initialStatus", oldStatus.name());
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, false);
            return "organization/opportunity-form";
        }

        // === Xác nhận publish khi DRAFT -> OPEN ===
        boolean needPublishConfirm =
                (requested == Opportunity.OpportunityStatus.OPEN) &&
                        (old == null || oldStatus == Opportunity.OpportunityStatus.DRAFT) &&
                        !"true".equals(confirmPublish);

        if (needPublishConfirm) {
            model.addAttribute("forcePublishConfirm", true);
            model.addAttribute("initialStatus", oldStatus.name());
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, false);
            return "organization/opportunity-form";
        }

        // === Map entity ===
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

        // === Upload thumbnail (nếu có) ===
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            String url = cloudStorage.uploadFile(form.getThumbnailFile());
            if (url == null) {
                binding.rejectValue("thumbnailFile", "upload.fail", "Upload ảnh thất bại");
                model.addAttribute("err", "Upload ảnh thất bại. Vui lòng thử lại.");
                model.addAttribute("initialStatus", oldStatus.name());
                populateCommon(model, form,
                        form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                        oldStatus, false);
                return "organization/opportunity-form";
            }
            // set vào form để hiển thị lại view; @Null không ảnh hưởng vì Bean Validation đã chạy trước đó
            form.setThumbnailUrl(url);
        }
        if (form.getThumbnailUrl() != null) {
            opp.setThumbnailUrl(form.getThumbnailUrl());
        }

        // === Sections: giữ ảnh cũ nếu không upload mới ===
        List<OpportunitySection> toSave = new ArrayList<>();
        int idx = 1;

        Map<Integer, OpportunitySection> oldByOrder = Collections.emptyMap();
        if (old != null) {
            List<OpportunitySection> existing = sectionService.findByOpportunity(old.getOppId());
            oldByOrder = new HashMap<>();
            for (OpportunitySection ex : existing) {
                Integer ord = (ex.getSectionOrder() != null ? ex.getSectionOrder() : 0);
                oldByOrder.put(ord, ex);
            }
        }

        for (int i = 0; i < form.getSections().size(); i++) {
            var sf = form.getSections().get(i);
            int order = (sf.getSectionOrder() != null ? sf.getSectionOrder() : idx);

            String finalImageUrl = null;
            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                String uploaded = cloudStorage.uploadFile(sf.getImageFile());
                if (uploaded == null) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.fail", "Upload ảnh thất bại");
                } else {
                    finalImageUrl = uploaded;
                }
            }

            if (finalImageUrl == null) {
                OpportunitySection oldSec = oldByOrder.get(order);
                if (oldSec != null && oldSec.getImageUrl() != null && !oldSec.getImageUrl().isBlank()) {
                    finalImageUrl = oldSec.getImageUrl();
                } else if (sf.getImageUrl() != null && !sf.getImageUrl().isBlank()) {
                    finalImageUrl = sf.getImageUrl();
                }
            }

            OpportunitySection s = new OpportunitySection();
            s.setOpportunity(opp);
            s.setSectionOrder(order);
            s.setHeading(sf.getHeading());
            s.setContent(sf.getContent());
            s.setCaption(sf.getCaption());
            s.setImageUrl(finalImageUrl);

            toSave.add(s);
            idx++;
        }

        if (binding.hasErrors()) {
            model.addAttribute("err", "Upload ảnh phần nội dung thất bại.");
            model.addAttribute("initialStatus", oldStatus.name());
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, false);
            return "organization/opportunity-form";
        }

        // === Lưu ===
        try {
            opp = opportunityService.save(opp);
            sectionService.replaceSections(opp, toSave);
        } catch (DataIntegrityViolationException | PersistenceException ex) {
            binding.reject("db.constraint", "Lưu thất bại do dữ liệu trùng lặp hoặc vi phạm ràng buộc.");
            model.addAttribute("err", "Lưu thất bại do dữ liệu không hợp lệ (ví dụ trùng Thứ tự).");
            model.addAttribute("initialStatus", oldStatus.name());
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, false);
            return "organization/opportunity-form";
        }

        // === Notify ===
        String publicLink = "/opportunities/" + opp.getOppId();
        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());

        OppSnapshot oldSnap = (old == null) ? null : OppSnapshot.from(old);

        if (oldSnap == null) {
            String title = "Cơ hội mới: " + opp.getTitle();
            String msg = buildCreateMessage(opp, organizationService.findByOwnerId(me.getUserId()));
            notificationService.notifyUsers(recipients, title, msg, "INFO", publicLink, me.getUserId(), opp.getOrganization().getOrgId());
            ra.addFlashAttribute("ok", "Tạo cơ hội thành công.");
        } else {
            String title = "Cập nhật cơ hội: " + opp.getTitle();
            String msg = buildUpdateMessage(oldSnap, opp, organizationService.findByOwnerId(me.getUserId()));
            if (!msg.isBlank()) {
                notificationService.notifyUsers(recipients, title, msg, "INFO", publicLink, me.getUserId(), opp.getOrganization().getOrgId());
            }
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

    /** Bổ sung allowedStatuses + readOnly/locked theo basisStatus và startedLock */
    private void populateCommon(Model model, OpportunityForm form, String title,
                                Opportunity.OpportunityStatus basisStatus,
                                boolean startedLock) {
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", title);

        var allowed = allowedStatusesFor(basisStatus);
        model.addAttribute("allowedStatuses", allowed);

        boolean locked = startedLock
                || basisStatus == Opportunity.OpportunityStatus.CANCELLED
                || basisStatus == Opportunity.OpportunityStatus.CLOSED;
        model.addAttribute("locked", locked);
        model.addAttribute("readOnly", locked);

        model.addAttribute("statuses", Opportunity.OpportunityStatus.values());
        model.addAttribute("statusVN", viStatus());
        model.addAttribute("categories", opportunityService.getCategoriesWithOpportunities());

        model.addAttribute("dynamicStatusLabel", computeDynamicLabel(
                basisStatus, form.getStartDate() != null && form.getStartTime() != null
                        ? LocalDateTime.of(form.getStartDate(), form.getStartTime()) : null,
                form.getEndDate() != null && form.getEndTime() != null
                        ? LocalDateTime.of(form.getEndDate(), form.getEndTime()) : null
        ));
    }

    private String computeDynamicLabel(Opportunity.OpportunityStatus st, LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (st == null) return "—";
        return switch (st) {
            case DRAFT -> "Bản nháp";
            case CANCELLED -> "Đã hủy";
            case CLOSED -> "Đã kết thúc";
            case OPEN -> {
                if (start != null && now.isBefore(start)) yield "Sắp diễn ra";
                if (start != null && end != null && (now.isEqual(start) || (now.isAfter(start) && now.isBefore(end))))
                    yield "Đang diễn ra";
                if (end != null && !now.isBefore(end)) yield "Đã kết thúc";
                yield "Đang mở";
            }
        };
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
        if (!Objects.equals(old.title, o.getTitle())) changes.add("Tiêu đề");
        if (!Objects.equals(old.subtitle, o.getSubtitle())) changes.add("Mô tả");
        if (!Objects.equals(old.location, o.getLocation())) changes.add("Địa điểm");
        if (!Objects.equals(old.neededVolunteers, o.getNeededVolunteers())) changes.add("Số lượng TNV");
        if (!Objects.equals(old.status, o.getStatus())) changes.add("Trạng thái");
        if (!Objects.equals(old.startTime, o.getStartTime()) || !Objects.equals(old.endTime, o.getEndTime()))
            changes.add("Thời gian");

        if (changes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Cơ hội \"").append(o.getTitle()).append("\" của tổ chức ").append(org.getName())
                .append(" đã được cập nhật (").append(String.join(", ", changes)).append("):\n")
                .append("• Từ: ").append(FMT.format(o.getStartTime())).append(" → ").append(FMT.format(o.getEndTime())).append("\n")
                .append("• Trạng thái: ").append(viStatus().getOrDefault(o.getStatus().name(), o.getStatus().name()));
        return sb.toString();
    }
}
