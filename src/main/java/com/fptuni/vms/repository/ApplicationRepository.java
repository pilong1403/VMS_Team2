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

        // Đếm số application hợp lệ của 1 opportunity
        long countByOppId(Integer oppId);

        // Cho phép lưu (merge) lại thông tin liên hệ của user
        User saveUser(User user);

        // LẤY DANH SÁCH ĐƠN CỦA VOLUNTEER — trả về Application + fetch join đủ dữ liệu
        List<Application> findAllByVolunteerId(Integer volunteerId);

        // ====== Truy vấn theo tổ chức (list + count + stats) — CÓ LỌC OPPID ======
        List<Application> findOrgApplications(Integer orgId,
                                              Integer oppId,
                                              String q,
                                              Application.ApplicationStatus status,
                                              LocalDateTime from,
                                              LocalDateTime to,
                                              int offset,
                                              int limit);

        long countOrgApplications(Integer orgId,
                                  Integer oppId,
                                  String q,
                                  Application.ApplicationStatus status,
                                  LocalDateTime from,
                                  LocalDateTime to);

        Map<Application.ApplicationStatus, Long> computeOrgAppStats(Integer orgId,
                                                                    Integer oppId,
                                                                    String q,
                                                                    Application.ApplicationStatus status,
                                                                    LocalDateTime from,
                                                                    LocalDateTime to);

        // ====== Lấy 1 application thuộc orgId (kèm fetch volunteer/opportunity) ======
        Application findByIdAndOrgId(Integer appId, Integer orgId);

        // ====== phục vụ gửi mail/thông báo khi opp hủy/sửa ======
        /** Trả về danh sách User đã được duyệt (APPROVED/COMPLETED) của 1 cơ hội. */
        List<User> findApprovedVolunteersByOppId(Integer oppId);

        /**
         * Trả về Application (đã duyệt) của 1 cơ hội, có fetch opportunity & organization (tiện build nội dung mail).
         */
        List<Application> findApprovedApplicationsByOppId(Integer oppId);

        // Đếm số ứng viên đã được duyệt của 1 cơ hội (APPROVED + COMPLETED)
        long countApprovedByOppId(Integer oppId);

        // ====== NEW: kiểm tra trùng thời gian với các đơn đang PENDING/APPROVED ======
        boolean hasOverlappingActiveApplications(Integer volunteerId,
                                                 LocalDateTime newStart,
                                                 LocalDateTime newEnd,
                                                 Integer excludeOppId);
}
