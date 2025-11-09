package com.fptuni.vms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.User;

public interface ApplicationService {
        Application apply(Integer oppId, Integer volunteerId, String reason);

        Application apply(Integer oppId, Integer volunteerId, String reason,
                        String fullName, String phone, String address);

        List<Application> listMyApplications(Integer volunteerId);

        // tìm kiếm/loc/sort + phân trang cho volunteer PhiLong
        Page<Application> searchMyApplications(Integer volunteerId,
                        String status,
                        String q,
                        String sort,
                        int page,
                        int size);

        // ====== ViewModel cho trang list của Organization ======
        // ĐÃ BỔ SUNG: reason, cancelReason, processedByName, processedAt, appliedAt ->
        // LocalDateTime
        record ApplicationRowVM(
                        Integer appId,
                        String volunteerName,
                        String volunteerAvatar,
                        String opportunityTitle,
                        LocalDateTime appliedAt,
                        String status,
                        String reason,
                        String cancelReason,
                        String processedByName,
                        LocalDateTime processedAt) {
        }

        // search + stats theo organization (CÓ LỌC OPPID)
        Page<ApplicationRowVM> searchOrgApplicationsByOrgId(Integer orgId,
                        Integer oppId,
                        String q,
                        String status,
                        LocalDate from,
                        LocalDate to,
                        int page,
                        int size);

        Map<String, Integer> computeOrgAppStats(Integer orgId,
                        Integer oppId,
                        String q,
                        String status,
                        LocalDate from,
                        LocalDate to);

        void approveApplication(Integer orgId, Integer appId, Integer processedById, String note);

        void rejectApplication(Integer orgId, Integer appId, Integer processedById, String note);

        List<User> findApprovedUsersByOppId(Integer oppId);

        long countApprovedByOppId(Integer oppId);

        boolean existsByOppIdAndVolunteerId(Integer oppId, Integer volunteerId);
}
