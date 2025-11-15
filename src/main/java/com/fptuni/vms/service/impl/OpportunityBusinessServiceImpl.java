package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.dto.request.OpportunitySectionForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.*;
import com.fptuni.vms.service.*;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class OpportunityBusinessServiceImpl implements OpportunityBusinessService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OpportunityService opportunityService;
    private final OpportunitySectionService sectionService;
    private final ApplicationService applicationService;
    private final OrganizationService organizationService;
    private final CloudStorageService cloudStorage;
    private final NotificationService notificationService;

    public OpportunityBusinessServiceImpl(OpportunityService opportunityService,
                                          OpportunitySectionService sectionService,
                                          ApplicationService applicationService,
                                          OrganizationService organizationService,
                                          CloudStorageService cloudStorage,
                                          NotificationService notificationService) {
        this.opportunityService = opportunityService;
        this.sectionService = sectionService;
        this.applicationService = applicationService;
        this.organizationService = organizationService;
        this.cloudStorage = cloudStorage;
        this.notificationService = notificationService;
    }

    // =================== PUBLIC API =================== //
    @Override
    public Opportunity saveOpportunityWithBusinessRules(
            OpportunityForm form,
            BindingResult binding,
            User currentUser
    ) {
        // Opp cũ (nếu có)
        Opportunity old = (form.getOppId() != null)
                ? opportunityService.findById(form.getOppId())
                : null;

        // 1) Chuẩn hóa dữ liệu text + XÓA SECTION RỖNG (Hướng 2)
        trimForm(form);

        // 2) Build thời gian bắt đầu / kết thúc
        LocalDateTime start = buildStart(form);
        LocalDateTime end   = buildEnd(form);

        // 3) Tìm Organization theo owner
        Organization org = organizationService.findByOwnerId(currentUser.getUserId());
        if (org == null) {
            binding.reject("org.missing", "Không tìm thấy thông tin tổ chức hợp lệ.");
        }

        // 4) Validate nghiệp vụ (time, slot, sections, status, lock)
        validateBusinessRules(form, old, org, start, end, binding);

        // Nếu đã có lỗi thì dừng, không map / save nữa
        if (binding.hasErrors()) {
            return null;
        }

        // 5) Check lock lần cuối (tránh race condition)
        if (old != null && isLockedForEdit(old)) {
            binding.reject("opp.locked", "Sự kiện đã bị khóa, không thể chỉnh sửa nữa.");
            return null;
        }

        // 6) Map form → entity
        Opportunity opp = (old == null) ? new Opportunity() : old;
        mapBasicFields(form, opp, org, start, end);

        // 7) Xử lý thumbnail
        if (!processThumbnail(form, opp, binding, old)) {
            return null;
        }

        // 8) Build danh sách sections để lưu
        List<OpportunitySection> sectionsToSave = buildSectionsForSave(form, opp, old, binding);
        if (binding.hasErrors()) {
            return null;
        }

        // 9) Lưu Opp + Sections
        try {
            opp = opportunityService.save(opp);
            sectionService.replaceSections(opp, sectionsToSave);
        } catch (DataIntegrityViolationException | PersistenceException ex) {
            binding.reject("db.constraint", "Lưu thất bại do dữ liệu trùng lặp hoặc vi phạm ràng buộc.");
            return null;
        }

        // 10) Gửi thông báo (nếu cần)
        sendNotifications(old, opp, org, currentUser);

        return opp;
    }

    @Override
    public void cancelOpportunity(Integer oppId, User actor) {
        Opportunity opp = opportunityService.findById(oppId);
        if (opp == null) {
            throw new IllegalStateException("Không tìm thấy sự kiện.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = opp.getStartTime();
        LocalDateTime end   = opp.getEndTime();

        // 1) Không cho hủy nếu không ở trạng thái OPEN
        if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN) {
            throw new IllegalStateException("Chỉ có thể hủy sự kiện đang ở trạng thái ĐANG MỞ.");
        }

        // 2) Đang diễn ra: start <= now < end  => không cho hủy
        if (start != null && end != null
                && !now.isBefore(start)   // now >= start
                && now.isBefore(end)) {   // now < end
            throw new IllegalStateException("Sự kiện đang diễn ra, không thể hủy.");
        }

        // 3) Đã kết thúc: now >= end  => không cho hủy
        if (end != null && !now.isBefore(end)) {
            throw new IllegalStateException("Sự kiện đã kết thúc, không thể hủy.");
        }

        // 4) Trường hợp còn lại: chưa diễn ra => cho phép hủy
        opp.setStatus(Opportunity.OpportunityStatus.CANCELLED);
        opportunityService.save(opp);

        Organization org = organizationService.findByOwnerId(actor.getUserId());
        Integer orgId = (org != null) ? org.getOrgId() : null;

        String title = "Thông báo hủy: " + opp.getTitle();
        String msg = "Cơ hội \"" + opp.getTitle() + "\" đã bị hủy bởi tổ chức "
                + (org != null ? org.getName() : "") + ". Rất mong bạn thông cảm.";
        String link = "/opportunities/" + opp.getOppId();

        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());
        notificationService.notifyUsers(recipients, title, msg, "ALERT", link,
                actor.getUserId(), orgId);
    }

    // =================== PRIVATE HELPERS =================== //

    private static Map<String, String> viStatus() {
        return Map.of(
                "DRAFT", "Lưu dưới dạng nháp",
                "OPEN", "Công khai sự kiên ",
                "CANCELLED", "Huỷ sự kiện",
                "CLOSED", "Đã kết thúc"
        );
    }

    private void trimForm(OpportunityForm form) {
        if (form.getTitle() != null)    form.setTitle(form.getTitle().trim());
        if (form.getSubtitle() != null) form.setSubtitle(form.getSubtitle().trim());
        if (form.getLocation() != null) form.setLocation(form.getLocation().trim());

        if (form.getSections() == null) {
            form.setSections(new ArrayList<>());
            return;
        }

        List<OpportunitySectionForm> cleaned = new ArrayList<>();
        for (OpportunitySectionForm s : form.getSections()) {
            // trim text
            if (s.getHeading() != null) s.setHeading(s.getHeading().trim());
            if (s.getContent() != null) s.setContent(s.getContent().trim());
            if (s.getCaption() != null) s.setCaption(s.getCaption().trim());

            // text trống hết
            boolean noText =
                    (s.getHeading() == null || s.getHeading().isBlank()) &&
                            (s.getContent() == null || s.getContent().isBlank()) &&
                            (s.getCaption() == null || s.getCaption().isBlank());

            // không chọn ảnh mới
            boolean noNewImage = (s.getImageFile() == null || s.getImageFile().isEmpty());

            // HƯỚNG 2: section rỗng khi không text + không ảnh mới
            // KHÔNG quan tâm imageUrl (ảnh cũ)
            boolean emptySection = noText && noNewImage;

            if (!emptySection) {
                cleaned.add(s);
            }
        }

        // ĐÁNH LẠI THỨ TỰ 1..N cho tất cả section còn lại
        int order = 1;
        for (OpportunitySectionForm s : cleaned) {
            s.setSectionOrder(order++);
        }

        form.setSections(cleaned);
    }



    private LocalDateTime buildStart(OpportunityForm form) {
        if (form.getStartDate() != null && form.getStartTime() != null) {
            return LocalDateTime.of(form.getStartDate(), form.getStartTime());
        }
        return null;
    }

    private LocalDateTime buildEnd(OpportunityForm form) {
        if (form.getEndDate() != null && form.getEndTime() != null) {
            return LocalDateTime.of(form.getEndDate(), form.getEndTime());
        }
        return null;
    }
    private void validateBusinessRules(OpportunityForm form,
                                       Opportunity old,
                                       Organization org,
                                       LocalDateTime start,
                                       LocalDateTime end,
                                       BindingResult binding) {

        // 0) Nếu org null thì đã reject ở ngoài, ở đây chỉ tiếp tục các rule khác
        // (không return sớm để trả đủ lỗi cho user nếu có thêm vấn đề khác)

        // 1) end > start
        if (start != null && end != null && !end.isAfter(start)) {
            binding.rejectValue("endDate", "invalid",
                    "Ngày/giờ kết thúc phải sau thời điểm bắt đầu");
        }

        // 2) start phải sau now + 2h nếu có thay đổi so với opp cũ
        if (start != null) {
            boolean startChanged = (old == null) || !start.equals(old.getStartTime());
            if (startChanged) {
                LocalDateTime minStart = LocalDateTime.now().plusHours(2);
                if (start.isBefore(minStart)) {
                    binding.rejectValue("startDate", "invalid.soon",
                            "Thời điểm bắt đầu phải sau ít nhất 2 giờ kể từ hiện tại");
                }
            }
        }

        // 3) Số TNV cần ≥ số TNV đã approved (nếu đang sửa opp)
        if (form.getOppId() != null) {
            long approved = applicationService.countApprovedApplications(form.getOppId());
            Integer needed = form.getNeededVolunteers();
            if (needed != null && approved > 0 && needed < approved) {
                binding.rejectValue(
                        "neededVolunteers",
                        "invalid.minApproved",
                        "Số TNV cần tối thiểu là " + approved +
                                " (hiện đã có " + approved + " tình nguyện viên đã được duyệt hoặc đang chờ xử lí!)."
                );
            }
        }

        // 4) Nếu trạng thái OPEN thì phải có ít nhất 1 section
        if (form.getStatus() == Opportunity.OpportunityStatus.OPEN &&
                (form.getSections() == null || form.getSections().isEmpty())) {
            binding.rejectValue(
                    "sections",
                    "sections.empty",
                    "Cơ hội ở trạng thái Đang mở phải có ít nhất 1 phần nội dung. Vui lòng thêm ít nhất 1 phần."
            );
        }

        // Đảm bảo không NPE (trimForm đã set list rỗng nếu null, nhưng cứ check cho chắc)
        if (form.getSections() == null) {
            form.setSections(new ArrayList<>());
        }

        // 5) Validate thứ tự section: >0 và không trùng
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < form.getSections().size(); i++) {
            OpportunitySectionForm sf = form.getSections().get(i);
            Integer ord = sf.getSectionOrder();
            if (ord == null || ord < 1) {
                binding.rejectValue("sections[" + i + "].sectionOrder", "order.invalid",
                        "Thứ tự phải là số dương (>=1)");
                continue;
            }
            if (!seen.add(ord)) {
                binding.rejectValue("sections[" + i + "].sectionOrder", "order.dup",
                        "Thứ tự bị trùng. Vui lòng chọn số khác.");
            }
        }

        // 6) BẮT BUỘC Tiêu đề & Nội dung cho CÁC SECTION CÒN LẠI (sau trimForm)
        for (int i = 0; i < form.getSections().size(); i++) {
            OpportunitySectionForm sf = form.getSections().get(i);

            // trimForm đã trim, nhưng vẫn check blank cho chắc
            if (sf.getHeading() == null || sf.getHeading().isBlank()) {
                binding.rejectValue(
                        "sections[" + i + "].heading",
                        "heading.blank",
                        "Tiêu đề phần là bắt buộc"
                );
            }

            if (sf.getContent() == null || sf.getContent().isBlank()) {
                binding.rejectValue(
                        "sections[" + i + "].content",
                        "content.blank",
                        "Mô tả chi tiết là bắt buộc"
                );
            }
        }

        // 7) Status chuyển đổi hợp lệ
        Opportunity.OpportunityStatus oldStatus =
                (old == null) ? Opportunity.OpportunityStatus.DRAFT : old.getStatus();
        Opportunity.OpportunityStatus requested = form.getStatus();

        List<Opportunity.OpportunityStatus> allowed = allowedStatusesFor(oldStatus);
        if (requested == null || !allowed.contains(requested)) {
            String msg = switch (oldStatus) {
                case DRAFT -> "Bản nháp chỉ có thể giữ DRAFT hoặc chuyển sang OPEN.";
                case OPEN -> "Chỉ được chọn OPEN hoặc CANCELLED (không thể quay về DRAFT).";
                case CANCELLED -> "Sự kiện đã HỦY, không thể đổi trạng thái.";
                case CLOSED -> "Sự kiện CLOSED đã bị khóa, không thể chỉnh sửa.";
            };
            binding.reject("status.invalid", msg);
        }

        // 8) Lock khi đã bắt đầu/CANCELLED/CLOSED
        if (old != null && isLockedForEdit(old)) {
            binding.reject("opp.locked", "Sự kiện đã bị khóa, không thể chỉnh sửa.");
        }
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

    private boolean isLockedForEdit(Opportunity opp) {
        boolean startedLock = opp.getStartTime() != null && !LocalDateTime.now().isBefore(opp.getStartTime());
        return startedLock
                || opp.getStatus() == Opportunity.OpportunityStatus.CANCELLED
                || opp.getStatus() == Opportunity.OpportunityStatus.CLOSED;
    }

    private void mapBasicFields(OpportunityForm form,
                                Opportunity opp,
                                Organization org,
                                LocalDateTime start,
                                LocalDateTime end) {
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
    }

    private boolean processThumbnail(OpportunityForm form,
                                     Opportunity opp,
                                     BindingResult binding,
                                     Opportunity old) {
        MultipartFile thumbFile = form.getThumbnailFile();
        if (thumbFile != null && !thumbFile.isEmpty()) {
            if (thumbFile.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(thumbFile.getContentType())) {
                binding.rejectValue("thumbnailFile", "upload.type",
                        "Chỉ chấp nhận tệp hình ảnh (jpg, png, gif, webp)");
                return false;
            }
            if (thumbFile.getSize() > MAX_IMAGE_BYTES) {
                binding.rejectValue("thumbnailFile", "upload.tooLarge", "Ảnh đại diện tối đa 5MB");
                return false;
            }

            String url = cloudStorage.uploadFile(thumbFile);
            if (url == null) {
                binding.rejectValue("thumbnailFile", "upload.fail", "Upload ảnh thất bại");
                return false;
            }
            form.setThumbnailUrl(url);
            opp.setThumbnailUrl(url);
        } else {
            String thumbFlag = form.getThumbnailUrl(); // null / "" / "__CLEAR__" / url
            boolean askedToClear = thumbFlag != null && (thumbFlag.isBlank() || "__CLEAR__".equals(thumbFlag));
            if (askedToClear) {
                opp.setThumbnailUrl(null);
                form.setThumbnailUrl(null);
            } else {
                if (thumbFlag != null) {
                    opp.setThumbnailUrl(thumbFlag);
                } else if (old != null) {
                    opp.setThumbnailUrl(old.getThumbnailUrl());
                    form.setThumbnailUrl(old.getThumbnailUrl());
                }
            }
        }
        return true;
    }

    private List<OpportunitySection> buildSectionsForSave(OpportunityForm form,
                                                          Opportunity opp,
                                                          Opportunity old,
                                                          BindingResult binding) {
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
            OpportunitySectionForm sf = form.getSections().get(i);
            int order = (sf.getSectionOrder() != null ? sf.getSectionOrder() : idx);

            String finalImageUrl = null;

            if (sf.getImageFile() != null && !sf.getImageFile().isEmpty()) {
                MultipartFile f = sf.getImageFile();
                if (f.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(f.getContentType())) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.type",
                            "Ảnh trong phần phải là hình (jpg, png, gif, webp)");
                } else if (f.getSize() > MAX_IMAGE_BYTES) {
                    binding.rejectValue("sections[" + i + "].imageFile", "upload.tooLarge",
                            "Ảnh trong phần tối đa 5MB");
                } else {
                    String uploaded = cloudStorage.uploadFile(f);
                    if (uploaded == null) {
                        binding.rejectValue("sections[" + i + "].imageFile", "upload.fail", "Upload ảnh thất bại");
                    } else {
                        finalImageUrl = uploaded;
                    }
                }
            } else {
                String cur = sf.getImageUrl(); // null / "" / "__CLEAR__" / url
                boolean askedToClear = cur != null && (cur.isBlank() || "__CLEAR__".equals(cur));
                if (askedToClear) {
                    finalImageUrl = null;
                } else if (cur != null) {
                    finalImageUrl = cur;
                } else {
                    OpportunitySection oldSec = oldByOrder.get(order);
                    if (oldSec != null && oldSec.getImageUrl() != null && !oldSec.getImageUrl().isBlank()) {
                        finalImageUrl = oldSec.getImageUrl();
                    }
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

        return toSave;
    }

    private void sendNotifications(Opportunity old,
                                   Opportunity opp,
                                   Organization org,
                                   User actor) {

        String publicLink = "/opportunities/" + opp.getOppId();
        List<User> recipients = applicationService.findApprovedUsersByOppId(opp.getOppId());

        if (old == null) {
            String title = "Cơ hội mới: " + opp.getTitle();
            String msg = buildCreateMessage(opp, org);
            notificationService.notifyUsers(recipients, title, msg, "INFO",
                    publicLink, actor.getUserId(), opp.getOrganization().getOrgId());
        } else {
            OppSnapshot oldSnap = OppSnapshot.from(old);
            String msg = buildUpdateMessage(oldSnap, opp, org);
            if (!msg.isBlank()) {
                String title = "Cập nhật cơ hội: " + opp.getTitle();
                notificationService.notifyUsers(recipients, title, msg, "INFO",
                        publicLink, actor.getUserId(), opp.getOrganization().getOrgId());
            }
        }
    }

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
        if (!Objects.equals(old.startTime, o.getStartTime()) ||
                !Objects.equals(old.endTime, o.getEndTime())) {
            changes.add("Thời gian");
        }

        if (changes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Cơ hội \"").append(o.getTitle()).append("\" của tổ chức ").append(org.getName())
                .append(" đã được cập nhật (").append(String.join(", ", changes)).append("):\n")
                .append("• Từ: ").append(FMT.format(o.getStartTime())).append(" → ").append(FMT.format(o.getEndTime())).append("\n")
                .append("• Trạng thái: ")
                .append(viStatus().getOrDefault(o.getStatus().name(), o.getStatus().name()));
        return sb.toString();
    }
}
