package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;
import jakarta.persistence.PersistenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

        private final ApplicationRepository applicationRepository;

        public ApplicationServiceImpl(ApplicationRepository applicationRepository) {
                this.applicationRepository = applicationRepository;
        }

        // Check cơ hội và tình nguyện viên, áp dụng các business rule, tạo application
        // mới
        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason) {
                Opportunity opp = applicationRepository.findOpportunityById(oppId);
                if (opp == null)
                        throw new IllegalArgumentException("Cơ hội không tồn tại: " + oppId);

                // Kiểm tra các business rule opportunity trước khi apply
                if (opp.getStatus() == Opportunity.OpportunityStatus.CANCELLED)
                        throw new IllegalStateException("Cơ hội đã bị hủy.");
                if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
                        throw new IllegalStateException("Cơ hội không còn mở.");

                if (opp.getStartTime() != null && !opp.getStartTime().isAfter(LocalDateTime.now()))
                        throw new IllegalStateException("Đã quá hạn đăng ký.");
                if (opp.getEndTime() != null && !opp.getEndTime().isAfter(LocalDateTime.now()))
                        throw new IllegalStateException("Cơ hội đã kết thúc.");

                User volunteer = applicationRepository.findUserById(volunteerId);
                if (volunteer == null)
                        throw new IllegalArgumentException("Tình nguyện viên không tồn tại: " + volunteerId);

                if (applicationRepository.existsByOppIdAndVolunteerId(oppId, volunteerId))
                        throw new IllegalStateException("Bạn đã ứng tuyển vào cơ hội này.");

                // check số lượng đăng ký đơn
                Integer need = opp.getNeededVolunteers();
                if (need != null) {
                        long active = applicationRepository.countByOppId(oppId);
                        if (active >= need)
                                throw new IllegalStateException("Cơ hội đã đủ số lượng đăng ký.");
                }

                // Check trùng thời gian, check PENDING/APPROVED của volunteer
                if (opp.getStartTime() != null && opp.getEndTime() != null) {
                        boolean overlapped = applicationRepository.hasOverlappingActiveApplications(
                                        volunteerId,
                                        opp.getStartTime(),
                                        opp.getEndTime(),
                                        opp.getOppId()); // tham số cơ hội mới
                        // nếu overlap = true thì throw
                        if (overlapped) {
                                throw new IllegalStateException(
                                                "Bạn đang có lịch trùng với một cơ hội khác đã đăng ký (đang chờ duyệt/đã duyệt).");
                        }
                }

                // Lưu application mới
                Application app = new Application();
                app.setOpportunity(opp);
                app.setVolunteer(volunteer);
                app.setAppliedAt(LocalDateTime.now());
                app.setReason(reason);
                app.setStatus(Application.ApplicationStatus.PENDING); // mặc định PENDING khi mới nộp
                app.setUpdatedAt(LocalDateTime.now());

                try {
                        return applicationRepository.save(app);
                } catch (PersistenceException ex) { /* PersistenceException khi duplicate key/constraint */
                        throw new IllegalStateException("Bạn đã ứng tuyển vào cơ hội này.");
                }
        }

        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason,
                        String fullName, String phone, String address) {
                var user = applicationRepository.findUserById(volunteerId);
                if (user == null)
                        throw new IllegalArgumentException("Volunteer not found: " + volunteerId);
                // cập nhật thông tin volunteer nếu có thay đổi
                boolean dirty = false;
                if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
                        user.setFullName(fullName);
                        dirty = true;
                }
                if (phone != null && !phone.isBlank() && !phone.equals(user.getPhone())) {
                        user.setPhone(phone);
                        dirty = true;
                }
                if (address != null && !address.isBlank() && !address.equals(user.getAddress())) {
                        user.setAddress(address);
                        dirty = true;
                }
                // dirty true thì save
                if (dirty)
                        applicationRepository.saveUser(user);

                return apply(oppId, volunteerId, reason);
        }

        // ========= VOLUNTEER VIEWS =========
        @Override
        public List<Application> listMyApplications(Integer volunteerId) {
                return applicationRepository.findAllByVolunteerId(volunteerId);
        }

        // (THÊM từ bản 2) tìm kiếm/lọc/sort + phân trang cho volunteer
        @Override
        public Page<Application> searchMyApplications(Integer volunteerId,
                        String status,
                        String q,
                        String sort,
                        int page,
                        int size) {
                var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

                Application.ApplicationStatus st = null;
                if (status != null && !status.isBlank()) {
                        try {
                                st = Application.ApplicationStatus.valueOf(status.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                        }
                }

                boolean newestFirst = !"oldest".equalsIgnoreCase(sort);

                List<Application> rows = applicationRepository.findMyApplications(
                                volunteerId, st, q, newestFirst ? "DESC" : "ASC",
                                pageable.getPageNumber() * pageable.getPageSize(),
                                pageable.getPageSize());
                long total = applicationRepository.countMyApplications(volunteerId, st, q);
                return new PageImpl<>(rows, pageable, total);
        }

        // ========= ORGANIZATION VIEWS =========
        @Override
        public Page<ApplicationRowVM> searchOrgApplicationsByOrgId(Integer orgId,
                        Integer oppId,
                        String q,
                        String status,
                        LocalDate from,
                        LocalDate to,
                        int page,
                        int size) {
                var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

                Application.ApplicationStatus st = null;
                if (status != null && !status.isBlank()) {
                        try {
                                st = Application.ApplicationStatus.valueOf(status.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                                /* keep null */ }
                }
                LocalDateTime fromDT = (from == null) ? null : from.atStartOfDay();
                LocalDateTime toDT = (to == null) ? null : to.plusDays(1).atStartOfDay(); // exclusive

                List<Application> rows = applicationRepository.findOrgApplications(
                                orgId, oppId, q, st, fromDT, toDT,
                                pageable.getPageNumber() * pageable.getPageSize(),
                                pageable.getPageSize());
                long total = applicationRepository.countOrgApplications(orgId, oppId, q, st, fromDT, toDT);
                // DÙNG VM ĐẦY ĐỦ (khớp UI modal chi tiết)
                List<ApplicationRowVM> vms = new ArrayList<>(rows.size());
                for (Application a : rows) {
                        var volunteer = a.getVolunteer();
                        var opp = a.getOpportunity();
                        vms.add(new ApplicationRowVM(
                                        a.getAppId(),
                                        (volunteer != null ? volunteer.getFullName() : "—"),
                                        (volunteer != null ? volunteer.getAvatarUrl() : null),
                                        (opp != null ? opp.getTitle() : "—"),
                                        // appliedAt dùng LocalDateTime để hiển thị HH:mm dd/MM/yyyy
                                        a.getAppliedAt(),
                                        (a.getStatus() != null ? a.getStatus().name() : "PENDING"),
                                        a.getReason(),
                                        a.getCancelReason(),
                                        (a.getProcessedBy() != null ? a.getProcessedBy().getFullName() : null),
                                        // processedAt: reuse updatedAt (phù hợp HTML đang dùng)
                                        a.getUpdatedAt()));
                }

                return new PageImpl<>(vms, pageable, total);
        }

        @Override
        public Map<String, Integer> computeOrgAppStats(Integer orgId,
                        Integer oppId,
                        String q,
                        String status,
                        LocalDate from,
                        LocalDate to) {
                Application.ApplicationStatus st = null;
                if (status != null && !status.isBlank()) {
                        try {
                                st = Application.ApplicationStatus.valueOf(status.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                                /* keep null */ }
                }
                LocalDateTime fromDT = (from == null) ? null : from.atStartOfDay();
                LocalDateTime toDT = (to == null) ? null : to.plusDays(1).atStartOfDay(); // exclusive

                Map<Application.ApplicationStatus, Long> m = applicationRepository.computeOrgAppStats(orgId, oppId, q,
                                st, fromDT, toDT);

                long total = 0, pending = 0, approved = 0, rejected = 0, completed = 0, cancelled = 0;
                for (var e : m.entrySet()) {
                        total += e.getValue();
                        switch (e.getKey()) {
                                case PENDING -> pending = e.getValue();
                                case APPROVED -> approved = e.getValue();
                                case REJECTED -> rejected = e.getValue();
                                case COMPLETED -> completed = e.getValue();
                                case CANCELLED -> cancelled = e.getValue();
                                default -> {
                                }
                        }
                }
                Map<String, Integer> out = new LinkedHashMap<>();
                out.put("total", (int) total);
                out.put("pending", (int) pending);
                out.put("approved", (int) approved);
                out.put("rejected", (int) rejected);
                out.put("completed", (int) completed);
                out.put("cancelled", (int) cancelled);
                return out;
        }

        // ========= APPROVE / REJECT =========
        @Override
        public void approveApplication(Integer orgId, Integer appId, Integer processedById, String note) {
                var app = applicationRepository.findByIdAndOrgId(appId, orgId);
                if (app == null)
                        throw new IllegalArgumentException("Không tìm thấy đơn hoặc không thuộc tổ chức.");
                if (app.getStatus() != Application.ApplicationStatus.PENDING)
                        throw new IllegalStateException("Chỉ có thể duyệt đơn đang chờ.");

                if (processedById != null) {
                        var user = applicationRepository.findUserById(processedById);
                        if (user != null)
                                app.setProcessedBy(user);
                }
                // Giữ nguyên logic cũ: note reuse vào cancelReason
                if (note != null && !note.isBlank())
                        app.setCancelReason(note.trim());

                app.setStatus(Application.ApplicationStatus.APPROVED);
                app.setUpdatedAt(LocalDateTime.now());
                applicationRepository.save(app);
        }

        @Override
        public void rejectApplication(Integer orgId, Integer appId, Integer processedById, String note) {
                var app = applicationRepository.findByIdAndOrgId(appId, orgId);
                if (app == null)
                        throw new IllegalArgumentException("Không tìm thấy đơn hoặc không thuộc tổ chức.");
                if (app.getStatus() != Application.ApplicationStatus.PENDING)
                        throw new IllegalStateException("Chỉ có thể từ chối đơn đang chờ.");

                if (processedById != null) {
                        var user = applicationRepository.findUserById(processedById);
                        if (user != null)
                                app.setProcessedBy(user);
                }
                if (note != null && !note.isBlank())
                        app.setCancelReason(note.trim());

                app.setStatus(Application.ApplicationStatus.REJECTED);
                app.setUpdatedAt(LocalDateTime.now());
                applicationRepository.save(app);
        }

        // ========= QUERY HELPERS =========
        @Override
        public List<User> findApprovedUsersByOppId(Integer oppId) {
                return applicationRepository.findApprovedVolunteersByOppId(oppId);
        }

        // Đếm số đơn PENDING/APPROVED/COMPLETED theo oppId
        @Override
        public long countApprovedByOppId(Integer oppId) {
                return applicationRepository.countApprovedByOppId(oppId);
        }

        @Override
        @Transactional(readOnly = true) // method chỉ đọc dữ liệu, không ghi
        public boolean existsByOppIdAndVolunteerId(Integer oppId, Integer volunteerId) {
                if (oppId == null || volunteerId == null)
                        return false;
                return applicationRepository.existsByOppIdAndVolunteerId(oppId, volunteerId);
        }

        @Override
        public void cancelByVolunteer(Integer appId, Integer volunteerId, String cancelReason) {
                if (appId == null || volunteerId == null)
                        throw new IllegalArgumentException("Thiếu thông tin đơn hoặc người dùng.");

                // Lấy application thuộc chính volunteer này (fetch opportunity & org để hiển
                // thị/nối rule)
                Application app = applicationRepository.findByIdAndVolunteerId(appId, volunteerId);
                if (app == null)
                        throw new IllegalArgumentException("Không tìm thấy đơn hoặc không thuộc sở hữu của bạn.");

                if (app.getStatus() != Application.ApplicationStatus.PENDING)
                        throw new IllegalStateException("Chỉ có thể hủy đơn đang chờ.");

                // Kiểm tra thời gian bắt đầu (trùng với rule DB để UX tốt hơn)
                var opp = app.getOpportunity();
                if (opp != null && opp.getStartTime() != null && !opp.getStartTime().isAfter(LocalDateTime.now())) {
                        throw new IllegalStateException("Không thể hủy sau khi cơ hội đã bắt đầu.");
                }

                if (cancelReason == null || cancelReason.trim().length() < 10)
                        throw new IllegalArgumentException("Lý do hủy cần tối thiểu 10 ký tự.");

                app.setStatus(Application.ApplicationStatus.CANCELLED);
                app.setCancelReason(cancelReason.trim());
                app.setUpdatedAt(LocalDateTime.now());

                applicationRepository.save(app);
        }

        @Override
        public long countApprovedApplications(Integer oppId) {
                return applicationRepository.countApprovedByOppId(oppId); // Sử dụng phương thức đã có trong repository
        }

        @Override
        public void cancelAllByOppId(Integer oppId, Integer processedById, String reason) {
                if (oppId == null) return;

                List<Application> apps = repo.findActiveApplicationsByOppId(oppId);
                if (apps.isEmpty()) return;

                User processedBy = null;
                if (processedById != null) {
                        processedBy = repo.findUserById(processedById);
                }

                String note = (reason == null || reason.isBlank())
                        ? "Đơn bị hủy do sự kiện đã bị hủy bởi tổ chức."
                        : reason.trim();

                LocalDateTime now = LocalDateTime.now();

                for (Application app : apps) {
                        app.setStatus(Application.ApplicationStatus.CANCELLED);
                        app.setCancelReason(note);
                        app.setUpdatedAt(now);
                        if (processedBy != null) {
                                app.setProcessedBy(processedBy);
                        }
                        repo.save(app);
                }
        }
}
