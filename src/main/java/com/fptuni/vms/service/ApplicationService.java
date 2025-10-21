package com.fptuni.vms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.fptuni.vms.model.Application;

public interface ApplicationService {
    Application apply(Integer oppId, Integer volunteerId, String reason);

    // apply kèm cập nhật nhanh thông tin liên hệ
    Application apply(Integer oppId, Integer volunteerId, String reason,
            String fullName, String phone, String address);

    // danh sách đơn của volunteer
    List<Application> listMyApplications(Integer volunteerId);

    // ====== PhiLong iter 2 ViewModel cho trang list của Organization ======
    record ApplicationRowVM(
            Integer appId,
            String volunteerName,
            String volunteerAvatar,
            String opportunityTitle,
            java.time.LocalDate appliedAt,
            String status) {
    }

    // ====== PhiLong iter 2 search + stats theo organization ======
    Page<ApplicationRowVM> searchOrgApplicationsByOrgId(Integer orgId,
            String q,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size);

    Map<String, Integer> computeOrgAppStats(Integer orgId);

    // ====== PhiLong iter2 duyệt / từ chối ======
    void approveApplication(Integer orgId, Integer appId, Integer processedById, String note);

    void rejectApplication(Integer orgId, Integer appId, Integer processedById, String note);
    // ================
}
