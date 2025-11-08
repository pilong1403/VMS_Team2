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

        // ====== NOTE: NEW — query cho volunteer có tìm kiếm/loc/sort + phân trang
        // ======
        List<Application> findMyApplications(Integer volunteerId,
                        Application.ApplicationStatus status,
                        String q,
                        String sortDir, // "ASC"/"DESC" theo appliedAt
                        int offset,
                        int limit);

        long countMyApplications(Integer volunteerId,
                        Application.ApplicationStatus status,
                        String q);

        // ====== theo tổ chức (giữ nguyên) ======
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

        List<Application> findApprovedApplicationsByOppId(Integer oppId);

        long countApprovedByOppId(Integer oppId);
}
