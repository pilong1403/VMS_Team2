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

        private final ApplicationRepository repo;

        public ApplicationServiceImpl(ApplicationRepository repo) {
                this.repo = repo;
        }

        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason) {
                Opportunity opp = repo.findOpportunityById(oppId);
                if (opp == null)
                        throw new IllegalArgumentException("Opportunity not found: " + oppId);

                if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
                        throw new IllegalStateException("Opportunity is not OPEN");

                if (opp.getEndTime() != null && !opp.getEndTime().isAfter(LocalDateTime.now()))
                        throw new IllegalStateException("Opportunity already ended");

                User volunteer = repo.findUserById(volunteerId);
                if (volunteer == null)
                        throw new IllegalArgumentException("Volunteer not found: " + volunteerId);

                if (repo.existsByOppIdAndVolunteerId(oppId, volunteerId))
                        throw new IllegalStateException("You already applied to this opportunity");

                Application app = new Application();
                app.setOpportunity(opp);
                app.setVolunteer(volunteer);
                app.setAppliedAt(LocalDateTime.now());
                app.setReason(reason);

                try {
                        return repo.save(app);
                } catch (PersistenceException ex) {
                        throw new IllegalStateException("You already applied to this opportunity");
                }
        }

        // @Override
        // public Application apply(Integer oppId, Integer volunteerId, String reason,
        // String fullName, String phone, String address) {

        // Opportunity opp = repo.findOpportunityById(oppId);
        // if (opp == null)
        // throw new IllegalArgumentException("Opportunity not found: " + oppId);
        // if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
        // throw new IllegalStateException("Opportunity is not OPEN");
        // if (opp.getEndTime() != null &&
        // !opp.getEndTime().isAfter(LocalDateTime.now()))
        // throw new IllegalStateException("Opportunity already ended");

        // User volunteer = repo.findUserById(volunteerId);
        // if (volunteer == null)
        // throw new IllegalArgumentException("Volunteer not found: " + volunteerId);

        // // Cập nhật nhanh thông tin liên hệ nếu người dùng có sửa trên form
        // boolean changed = false;
        // if (fullName != null && !fullName.isBlank() &&
        // !fullName.equals(volunteer.getFullName())) {
        // volunteer.setFullName(fullName.trim());
        // changed = true;
        // }
        // if (phone != null && !phone.isBlank() && !phone.equals(volunteer.getPhone()))
        // {
        // volunteer.setPhone(phone.trim());
        // changed = true;
        // }
        // if (address != null && !address.isBlank() &&
        // !address.equals(volunteer.getAddress())) {
        // volunteer.setAddress(address.trim());
        // changed = true;
        // }
        // if (changed)
        // repo.saveUser(volunteer);

        // if (repo.existsByOppIdAndVolunteerId(oppId, volunteerId))
        // throw new IllegalStateException("You already applied to this opportunity");

        // Application app = new Application();
        // app.setOpportunity(opp);
        // app.setVolunteer(volunteer);
        // app.setAppliedAt(LocalDateTime.now());
        // app.setReason(reason);

        // try {
        // return repo.save(app);
        // } catch (PersistenceException ex) {
        // throw new IllegalStateException("You already applied to this opportunity");
        // }
        // }

        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason,
                        String fullName, String phone, String address) {
                // cập nhật thông tin user nếu FE gửi lên từ popup (không ép buộc)
                var user = repo.findUserById(volunteerId);
                if (user == null)
                        throw new IllegalArgumentException("Volunteer not found: " + volunteerId);
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
                if (dirty)
                        repo.saveUser(user);

                // gọi lại apply gốc để tái dùng toàn bộ rule/trigger kiểm tra
                return apply(oppId, volunteerId, reason);
        }

        @Override
        public java.util.List<Application> listMyApplications(Integer volunteerId) {
                return repo.findAllByVolunteerId(volunteerId);
        }

        // ================= Phi Long iter 2: list theo orgId =================
        @Override
        public Page<ApplicationRowVM> searchOrgApplicationsByOrgId(Integer orgId,
                        String q,
                        String status,
                        LocalDate from,
                        LocalDate to,
                        int page,
                        int size) {
                var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

                // map filter
                Application.ApplicationStatus st = null;
                if (status != null && !status.isBlank()) {
                        try {
                                st = Application.ApplicationStatus.valueOf(status.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                                /* giữ null để coi như "all" */ }
                }
                LocalDateTime fromDT = (from == null) ? null : from.atStartOfDay();
                LocalDateTime toDT = (to == null) ? null : to.plusDays(1).atStartOfDay(); // exclusive

                // query
                List<Application> rows = repo.findOrgApplications(
                                orgId, q, st, fromDT, toDT,
                                pageable.getPageNumber() * pageable.getPageSize(),
                                pageable.getPageSize());
                long total = repo.countOrgApplications(orgId, q, st, fromDT, toDT);

                // map -> VM
                List<ApplicationRowVM> vms = new ArrayList<>(rows.size());
                for (Application a : rows) {
                        var volunteer = a.getVolunteer();
                        var opp = a.getOpportunity();
                        vms.add(new ApplicationRowVM(
                                        a.getAppId(),
                                        volunteer != null ? volunteer.getFullName() : "—",
                                        volunteer != null ? volunteer.getAvatarUrl() : null,
                                        opp != null ? opp.getTitle() : "—",
                                        a.getAppliedAt() != null ? a.getAppliedAt().toLocalDate() : null,
                                        a.getStatus() != null ? a.getStatus().name() : "PENDING"));
                }

                return new PageImpl<>(vms, pageable, total);
        }

        @Override
        public Map<String, Integer> computeOrgAppStats(Integer orgId) {
                Map<Application.ApplicationStatus, Long> m = repo.computeOrgAppStats(orgId);
                // ép về key như template đang dùng: total/pending/approved/rejected
                long total = 0, pending = 0, approved = 0, rejected = 0;
                for (var e : m.entrySet()) {
                        total += e.getValue();
                        switch (e.getKey()) {
                                case PENDING -> pending = e.getValue();
                                case APPROVED -> approved = e.getValue();
                                case REJECTED -> rejected = e.getValue();
                                default -> {
                                }
                        }
                }
                Map<String, Integer> out = new LinkedHashMap<>();
                out.put("total", (int) total);
                out.put("pending", (int) pending);
                out.put("approved", (int) approved);
                out.put("rejected", (int) rejected);
                return out;
        }

        // ================= Phi Long iter 2: duyệt / từ chối đơn =================
        @Override
        public void approveApplication(Integer orgId, Integer appId, Integer processedById, String note) {
                var app = repo.findByIdAndOrgId(appId, orgId);
                if (app == null)
                        throw new IllegalArgumentException("Không tìm thấy đơn hoặc không thuộc tổ chức.");
                if (app.getStatus() != Application.ApplicationStatus.PENDING)
                        throw new IllegalStateException("Chỉ có thể duyệt đơn đang chờ.");

                if (processedById != null) {
                        var user = repo.findUserById(processedById);
                        if (user != null)
                                app.setProcessedBy(user);
                }
                // Tạm dùng cancelReason để lưu ghi chú xử lý
                if (note != null && !note.isBlank())
                        app.setCancelReason(note.trim());

                app.setStatus(Application.ApplicationStatus.APPROVED);
                app.setUpdatedAt(LocalDateTime.now());
                repo.save(app);
        }

        @Override
        public void rejectApplication(Integer orgId, Integer appId, Integer processedById, String note) {
                var app = repo.findByIdAndOrgId(appId, orgId);
                if (app == null)
                        throw new IllegalArgumentException("Không tìm thấy đơn hoặc không thuộc tổ chức.");
                if (app.getStatus() != Application.ApplicationStatus.PENDING)
                        throw new IllegalStateException("Chỉ có thể từ chối đơn đang chờ.");

                if (processedById != null) {
                        var user = repo.findUserById(processedById);
                        if (user != null)
                                app.setProcessedBy(user);
                }
                // Lưu lý do từ chối
                if (note != null && !note.isBlank())
                        app.setCancelReason(note.trim());

                app.setStatus(Application.ApplicationStatus.REJECTED);
                app.setUpdatedAt(LocalDateTime.now());
                repo.save(app);
        }
        // ===========================================================

}
