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
import java.util.*;

@Controller
@RequestMapping("/org/opps")
public class OpportunityController {

    // Service CRUD cơ hội (opportunities)
    private final OpportunityService opportunityService;
    // Service CRUD các section mô tả chi tiết cho cơ hội
    private final OpportunitySectionService sectionService;
    // Service xử lý thông tin tổ chức
    private final OrganizationService organizationService;
    // Business service gom toàn bộ rule nghiệp vụ
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

    // Map dùng cho FORM (select trạng thái trong form cơ hội)
    private static Map<String, String> viStatusForm() {
        return Map.of(
                "DRAFT", "Lưu dưới dạng nháp",
                "OPEN", "Công khai sự kiện",
                "CANCELLED", "Huỷ sự kiện",
                "CLOSED", "Đã kết thúc"
        );
    }

    // Map dùng cho LIST/FILTER (dropdown lọc ở trang danh sách)
    private static Map<String, String> viStatusFilter() {
        return Map.of(
                "DRAFT", "Bản nháp",
                "OPEN", "Sự kiện đang mở",
                "CANCELLED", "Đã hủy",
                "CLOSED", "Đã kết thúc"
        );
    }


    /**
     * Trả về list các trạng thái mà user được phép chọn,
     * dựa trên trạng thái hiện tại (rule chuyển trạng thái đơn giản).
     */
    private List<Opportunity.OpportunityStatus> allowedStatusesFor(Opportunity.OpportunityStatus current) {
        if (current == null) {
            // Cơ hội mới tạo (chưa có status) -> cho DRAFT và OPEN
            return List.of(Opportunity.OpportunityStatus.DRAFT, Opportunity.OpportunityStatus.OPEN);
        }
        return switch (current) {
            case DRAFT -> List.of(
                    Opportunity.OpportunityStatus.DRAFT,
                    Opportunity.OpportunityStatus.OPEN
            );
            case OPEN -> List.of(
                    Opportunity.OpportunityStatus.OPEN,
                    Opportunity.OpportunityStatus.CANCELLED
            );
            case CANCELLED -> List.of(
                    Opportunity.OpportunityStatus.CANCELLED
            );
            case CLOSED -> List.of(
                    Opportunity.OpportunityStatus.CLOSED
            );
        };
    }

    // ================= LIST =================

    /**
     * GET /org/opps
     * - Danh sách cơ hội của chủ tổ chức hiện tại (ORG_OWNER).
     * - Hỗ trợ filter theo từ khóa, status, sort theo time, phân trang.
     */
    @GetMapping
    public String listForOwner(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "status", required = false) Opportunity.OpportunityStatus status,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @RequestParam(value = "timeOrder", required = false) String timeOrder,
                               Model model) {
        model.addAttribute("activePage", "OppManagement");

        // Lấy user hiện tại từ SecurityContext
        User me = SecurityUtils.getCurrentUser();
        // Lấy organization mà user hiện tại là owner
        Organization org = organizationService.findByOwnerId(me.getUserId());
        if (org == null) {
            // Không có org hợp lệ -> báo lỗi + trả về view rỗng
            model.addAttribute("error", "Không tìm thấy thông tin tổ chức hợp lệ. Vui lòng kiểm tra hoặc liên hệ quản trị viên.");
            model.addAttribute("page", Page.empty());
            model.addAttribute("statusVN", viStatusFilter());
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

        // Page trong Spring Data là zero-based, trong UI là 1-based
        int zeroBased = Math.max(page - 1, 0);
        Page<Opportunity> result = opportunityService.searchByOrg(
                org.getOrgId(), q, status, zeroBased, size, timeOrder
        );

        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statusVN", viStatusFilter());
        model.addAttribute("timeOrder", timeOrder);

        // Tính toán thông tin phân trang để hiển thị nút [1][2][3]...
        int currentPage = result.getNumber() + 1; // convert lại thành 1-based
        int totalPages = result.getTotalPages() == 0 ? 1 : result.getTotalPages();
        int window = 5; // hiển thị tối đa 5 trang trên thanh phân trang
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

    /**
     * GET /org/opps/new
     * - Hiển thị form tạo cơ hội mới.
     * - Khởi tạo 1 section trống ban đầu, status mặc định là DRAFT.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        OpportunityForm form = new OpportunityForm();

        // Thêm 1 section rỗng mặc định cho form
        var s0 = new OpportunitySectionForm();
        s0.setSectionOrder(1);
        form.getSections().add(s0);

        // Cơ hội mới luôn bắt đầu ở trạng thái DRAFT
        form.setStatus(Opportunity.OpportunityStatus.DRAFT);

        populateCommon(model, form, "Tạo cơ hội mới", Opportunity.OpportunityStatus.DRAFT, false);
        model.addAttribute("initialStatus", Opportunity.OpportunityStatus.DRAFT.name());
        return "organization/opportunity-form";
    }

    /**
     * GET /org/opps/{id}/edit
     * - Hiển thị form chỉnh sửa cơ hội.
     * - Nếu cơ hội đã bắt đầu / đã huỷ / đã kết thúc → lock, chỉ xem chi tiết.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Opportunity opp = opportunityService.findById(id);
        if (opp == null) {
            ra.addFlashAttribute("err", "Không tìm thấy cơ hội.");
            return "redirect:/org/opps";
        }

        // startedLock: sự kiện đã bắt đầu (now >= startTime)
        boolean startedLock = opp.getStartTime() != null && !LocalDateTime.now().isBefore(opp.getStartTime());
        // cancelledLock: đã CANCELLED
        boolean cancelledLock = opp.getStatus() == Opportunity.OpportunityStatus.CANCELLED;
        // closedLock: đã CLOSED
        boolean closedLock = opp.getStatus() == Opportunity.OpportunityStatus.CLOSED;
        // locked: nếu true thì form chuyển sang mode chỉ đọc
        boolean locked = startedLock || cancelledLock || closedLock;

        // Map entity sang form + lấy các section
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

    /**
     * POST /org/opps/save
     * - Tạo / cập nhật cơ hội.
     * - Flow:
     *   1) Nếu request muốn publish (OPEN) cơ hội đang là DRAFT mà chưa confirm → bật modal confirm.
     *   2) Gọi OpportunityBusinessService để xử lý toàn bộ rule nghiệp vụ (validate thời gian, status, ...).
     *   3) Nếu có lỗi -> quay lại form + hiển thị lỗi.
     *   4) Nếu OK -> redirect về trang edit của cơ hội vừa lưu.
     */
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OpportunityForm form,
                       BindingResult binding,
                       RedirectAttributes ra,
                       Model model,
                       @RequestParam(value = "confirmPublish", defaultValue = "false") String confirmPublish) {

        User me = SecurityUtils.getCurrentUser();
        // Nếu oppId != null => là update, cần lấy old để biết oldStatus + lock
        Opportunity old = (form.getOppId() != null) ? opportunityService.findById(form.getOppId()) : null;

        Opportunity.OpportunityStatus oldStatus =
                (old == null) ? Opportunity.OpportunityStatus.DRAFT : old.getStatus();
        Opportunity.OpportunityStatus requested = form.getStatus();

        // ===== Bước 1: xử lý confirm publish (OPEN từ DRAFT) =====
        // Điều kiện: user yêu cầu OPEN lần đầu (từ DRAFT/new) mà chưa tick confirmPublish
        boolean needPublishConfirm =
                (requested == Opportunity.OpportunityStatus.OPEN) &&
                        (old == null || oldStatus == Opportunity.OpportunityStatus.DRAFT) &&
                        !"true".equals(confirmPublish);

        if (needPublishConfirm) {
            // set flag cho view bật modal confirm
            model.addAttribute("forcePublishConfirm", true);
            model.addAttribute("initialStatus", oldStatus.name());

            boolean startedLock = false;
            if (old != null && old.getStartTime() != null) {
                startedLock = !LocalDateTime.now().isBefore(old.getStartTime());
            }

            // Chuẩn bị dữ liệu chung cho form (categories, status VN, allowed status, ...)
            populateCommon(model, form,
                    form.getOppId() == null ? "Tạo cơ hội mới" : "Chỉnh sửa cơ hội",
                    oldStatus, startedLock);

            // Quay lại form, view sẽ hiển thị modal confirm publish
            return "organization/opportunity-form";
        }

        // ===== Bước 2: Gọi service xử lý toàn bộ nghiệp vụ (validate + save) =====
        //  - Service sẽ tự:
        //      + Kiểm tra quyền sở hữu
        //      + Kiểm tra rule thời gian (start < end, không sửa khi đã bắt đầu, ...)
        //      + Xử lý status (DRAFT/OPEN/CANCELLED/CLOSED)
        //      + Lưu Opportunity + sections
        Opportunity opp = opportunityBusinessService.saveOpportunityWithBusinessRules(form, binding, me);

        // Nếu có lỗi binding hoặc service trả null => quay lại form
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

        // ===== Bước 3: OK -> flash message + redirect về trang edit của opp đó =====
        ra.addFlashAttribute("ok",
                form.getOppId() == null ? "Tạo cơ hội thành công." : "Cập nhật cơ hội thành công.");

        return "redirect:/org/opps/%d/edit".formatted(opp.getOppId());
    }

    // ================= CANCEL =================

    /**
     * POST /org/opps/{id}/cancel
     * - Huỷ sự kiện với đầy đủ rule nghiệp vụ ở OpportunityBusinessService:
     *   + Chỉ cho huỷ khi đang OPEN
     *   + Kiểm tra quyền owner
     *   + Gửi thông báo đến volunteer (nếu bạn đã implement)
     */
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

    /**
     * Hàm helper dùng chung để đẩy các dữ liệu cần thiết cho form create/edit:
     *  - form: dữ liệu form
     *  - pageTitle: tiêu đề trang
     *  - basisStatus: trạng thái "gốc" của cơ hội (để tính allowedStatuses)
     *  - startedLock: true nếu sự kiện đã bắt đầu (dùng để tính locked)
     */
    private void populateCommon(Model model, OpportunityForm form, String title,
                                Opportunity.OpportunityStatus basisStatus,
                                boolean startedLock) {
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", title);

        // allowedStatuses: các status có thể chọn từ trạng thái basisStatus
        var allowed = allowedStatusesFor(basisStatus);
        model.addAttribute("allowedStatuses", allowed);

        // locked (readOnly) nếu đã bắt đầu, đã CANCELLED hoặc đã CLOSED
        boolean locked = startedLock
                || basisStatus == Opportunity.OpportunityStatus.CANCELLED
                || basisStatus == Opportunity.OpportunityStatus.CLOSED;
        model.addAttribute("locked", locked);
        model.addAttribute("readOnly", locked);

        // Toàn bộ enum status (nếu cần trong view)
        model.addAttribute("statuses", Opportunity.OpportunityStatus.values());
        // Map status -> tiếng Việt
        model.addAttribute("statusVN", viStatusForm());
        // Danh sách category có cơ hội (hoặc toàn bộ category tùy implement)
        model.addAttribute("categories", opportunityService.getCategoriesWithOpportunities());
        // Lưu lại basisStatus để view dùng
        model.addAttribute("basisStatus", basisStatus);
    }

    /**
     * Map entity Opportunity + list OpportunitySection sang OpportunityForm
     * để hiển thị ở form edit.
     */
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
            // Nếu entity đã có sectionOrder thì dùng, nếu không thì gán tạm theo i
            sf.setSectionOrder(s.getSectionOrder() != null ? s.getSectionOrder() : i);
            sf.setHeading(s.getHeading());
            sf.setContent(s.getContent());
            sf.setImageUrl(s.getImageUrl());
            sf.setCaption(s.getCaption());
            sfs.add(sf);
            i++;
        }

        // Nếu cơ hội chưa có section nào -> thêm 1 section rỗng để form luôn có 1 block
        if (sfs.isEmpty()) {
            var one = new OpportunitySectionForm();
            one.setSectionOrder(1);
            sfs.add(one);
        }

        f.setSections(sfs);
        return f;
    }
}
