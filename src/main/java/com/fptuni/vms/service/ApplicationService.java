package com.fptuni.vms.service;

import java.time.LocalDate;
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

        // NOTE: NEW — tìm kiếm/loc/sort + phân trang cho volunteer
        Page<Application> searchMyApplications(Integer volunteerId,
                        String status,
                        String q,
                        String sort, // "newest" | "oldest"
                        int page,
                        int size);

        // ====== ViewModel cho trang list của Organization ======
        record ApplicationRowVM(
                        Integer appId,
                        String volunteerName,
                        String volunteerAvatar,
                        String opportunityTitle,
                        java.time.LocalDate appliedAt,
                        String status) {
        }

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
}
