package com.fptuni.vms.controller;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.dto.request.OpportunitySectionForm;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.security.SecurityUtils;
import com.fptuni.vms.service.OpportunityBusinessService;
import com.fptuni.vms.service.OpportunitySectionService;
import com.fptuni.vms.service.OpportunityService;
import com.fptuni.vms.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/org/opps")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final OpportunitySectionService sectionService;
    private final OrganizationService organizationService;
    private final OpportunityBusinessService opportunityBusinessService;

    public OpportunityController(OpportunityService opportunityService,
                                 OpportunitySectionService sectionService,
                                 OrganizationService organizationService,
                                 OpportunityBusinessService opportunityBusinessService) {
        this.opportunityService = opportunityService;
        this.sectionService = sectionService;
        this.organizationService = organizationService;
        this.opportunityBusinessService = opportunityBusinessService;
    }

    // ================= COMMON MAPPING =================

    private static Map<String, String> viStatus() {
        return Map.of(
                "DRAFT", "Lưu dưới dạng nháp",
                "OPEN", "Công khai sự kiên ",
                "CANCELLED", "Huỷ sự kiện",
                "CLOSED", "Đã kết thúc"
        );
    }

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

    // ================= LIST =================

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

    // ================= NEW / EDIT FORM =================

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
        if (opp == null) {
            ra.addFlashAttribute("err", "Không tìm thấy cơ hội.");
            return "redirect:/org/opps";
        }

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

    // ================= SAVE =================

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OpportunityForm form,
                       BindingResult binding,
                       RedirectAttributes ra,
                       Model model,
                       @RequestParam(value = "confirmPublish", defaultValue = "false") String confirmPublish) {

        User me = SecurityUtils.getCurrentUser();
        Opportunity old = (form.getOppId() != null) ? opportunityService.findById(form.getOppId()) : null;

        Opportunity.OpportunityStatus oldStatus =
                (old == null) ? Opportunity.OpportunityStatus.DRAFT : old.getStatus();
        Opportunity.OpportunityStatus requested = form.getStatus();

        // Giữ nguyên logic confirm publish (modal) ở controller
        boolean needPublishConfirm =
                (requested == Opportunity.OpportunityStatus.OPEN) &&
                        (old == null || oldStatus == Opportunity.OpportunityStatus.DRAFT) &&
                        !"true".equals(confirmPublish);

        if (needPublishConfirm) {
            model.addAttribute("forcePublishConfirm", true);
            model.addAttribute("initialStatus", oldStatus.name());

            boolean startedLock = false;
            if (old != null && old.getStartTime() != null) {
                startedLock = !LocalDateTime.now().isBefore(old.getStartTime());
            }

            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, startedLock);

            return "organization/opportunity-form";
        }

        // Gọi service xử lý toàn bộ nghiệp vụ
        Opportunity opp = opportunityBusinessService.saveOpportunityWithBusinessRules(form, binding, me);

        if (binding.hasErrors() || opp == null) {
            Opportunity.OpportunityStatus basis =
                    (form.getOppId() == null)
                            ? Opportunity.OpportunityStatus.DRAFT
                            : oldStatus;

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

        ra.addFlashAttribute("ok",
                form.getOppId() == null ? "Tạo cơ hội thành công." : "Cập nhật cơ hội thành công.");

        return "redirect:/org/opps/%d/edit".formatted(opp.getOppId());
    }

    // ================= CANCEL =================

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id, RedirectAttributes ra) {
        User me = SecurityUtils.getCurrentUser();
        try {
            opportunityBusinessService.cancelOpportunity(id, me);
            ra.addFlashAttribute("ok", "Đã hủy sự kiện và gửi thông báo đến tình nguyện viên.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("err", ex.getMessage());
        }
        return "redirect:/org/opps";
    }

    // ================= HELPERS =================

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
        model.addAttribute("basisStatus", basisStatus);
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
}
