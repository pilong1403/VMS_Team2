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

        long countByOppId(Integer oppId);

        User saveUser(User user);

        List<Application> findAllByVolunteerId(Integer volunteerId);

        List<Application> findMyApplications(Integer volunteerId,
                        Application.ApplicationStatus status,
                        String q,
                        String sortDir,
                        int offset,
                        int limit);

        long countMyApplications(Integer volunteerId,
                        Application.ApplicationStatus status,
                        String q);

        // theo tổ chức (giữ nguyên)
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

        Application findByIdAndOrgId(Integer appId, Integer orgId);

        List<User> findApprovedVolunteersByOppId(Integer oppId);

        /**
         * Trả về Application (đã duyệt) của 1 cơ hội, có fetch opportunity &
         * organization (tiện build nội dung mail).
         */
        List<Application> findApprovedApplicationsByOppId(Integer oppId);

        long countApprovedByOppId(Integer oppId);

        // ====== NEW: kiểm tra trùng thời gian với các đơn đang PENDING/APPROVED ======
        boolean hasOverlappingActiveApplications(Integer volunteerId,
                        LocalDateTime newStart,
                        LocalDateTime newEnd,
                        Integer excludeOppId);

        Application findByIdAndVolunteerId(Integer appId, Integer volunteerId);

        long countApprovedApplications(Integer oppId);

        List<Application> findActiveApplicationsByOppId(Integer oppId);

}
