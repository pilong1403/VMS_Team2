package com.fptuni.vms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;

public interface ApplicationRepository {
        boolean existsByOppIdAndVolunteerId(Integer oppId, Integer volunteerId);

        Application save(Application application);

        Opportunity findOpportunityById(Integer oppId);

        User findUserById(Integer userId);

        // đếm số application hợp lệ của 1 opportunity
        long countByOppId(Integer oppId);

        // cho phép lưu (merge) lại thông tin liên hệ của user
        User saveUser(User user);

        // LẤY DANH SÁCH ĐƠN CỦA VOLUNTEER — trả về Application + fetch join đủ dữ liệu
        List<Application> findAllByVolunteerId(Integer volunteerId);

        // ====== PhiLong iter2 Query theo tổ chức (list + count + stats) ======
        List<Application> findOrgApplications(Integer orgId,
                        String q,
                        Application.ApplicationStatus status,
                        LocalDateTime from,
                        LocalDateTime to,
                        int offset,
                        int limit);

        long countOrgApplications(Integer orgId,
                        String q,
                        Application.ApplicationStatus status,
                        LocalDateTime from,
                        LocalDateTime to);

        Map<Application.ApplicationStatus, Long> computeOrgAppStats(Integer orgId);

        // ======PhiLong iter2 :lấy 1 application thuộc orgId (kèm fetch
        // volunteer/opportunity) phê duyệt đơn======
        Application findByIdAndOrgId(Integer appId, Integer orgId);
        // ========================================
}
