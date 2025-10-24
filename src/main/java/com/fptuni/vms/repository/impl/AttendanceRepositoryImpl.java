package com.fptuni.vms.repository.impl;

import com.fptuni.vms.dto.response.AttendanceRecordDTO;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Attendance;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.AttendanceRepository;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@Transactional
public class AttendanceRepositoryImpl implements AttendanceRepository {

    @PersistenceContext
    private EntityManager em;


    @Override
    public List<Opportunity> filterOpportunitiesByOrg(long orgId, String status, String keyword, String timeOrder, int page, int size) {
        StringBuilder jpqlBuilder = new StringBuilder("SELECT o FROM Opportunity o WHERE o.organization.id = :orgId");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("orgId", orgId);

        if ((status != null && !status.trim().isEmpty()) && !status.equalsIgnoreCase("ALL")) {
            jpqlBuilder.append(" AND o.status = :status");
            parameters.put("status", Opportunity.OpportunityStatus.valueOf(status.toUpperCase()));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpqlBuilder.append(" AND (LOWER(o.title) LIKE LOWER(:keyword))");
            parameters.put("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }

        if ("asc".equalsIgnoreCase(timeOrder)) {
            jpqlBuilder.append(" ORDER BY o.startTime ASC");
        } else if ("desc".equalsIgnoreCase(timeOrder)) {
            jpqlBuilder.append(" ORDER BY o.startTime DESC");
        }
        TypedQuery<Opportunity> query = em.createQuery(jpqlBuilder.toString(), Opportunity.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public long countOppAfterFilteredByOrg(long orgId, String status, String keyword) {
        StringBuilder jpqlBuilder = new StringBuilder("SELECT COUNT(o) FROM Opportunity o WHERE o.organization.id = :orgId");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("orgId", orgId);

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            jpqlBuilder.append(" AND o.status = :status");
            parameters.put("status", Opportunity.OpportunityStatus.valueOf(status.trim().toUpperCase()));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            jpqlBuilder.append(" AND (LOWER(o.title) LIKE LOWER(:keyword))");
            parameters.put("keyword", "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
        TypedQuery<Long> query = em.createQuery(jpqlBuilder.toString(), Long.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query.getSingleResult();
    }

    @Override
    public Opportunity getOpportunity(long id) {
        return em.find(Opportunity.class, id);
    }

    @Override
    public Organization findOrganizationByOwnerId(Integer ownerId) {
        try {
            TypedQuery<Organization> query = em.createQuery(
                    "SELECT o FROM Organization o WHERE o.owner.id = :ownerId",
                    Organization.class
            );
            query.setParameter("ownerId", ownerId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<AttendanceRecordDTO> getAllAttendanceRecordsForOpportunity(Integer opportunityId) {
        String jpql = """
            SELECT new com.fptuni.vms.dto.response.AttendanceRecordDTO(
                app.appId, att.attId, u.userId, u.fullName, u.email, u.phone,
                u.address, u.avatarUrl, att.checkinTime, att.checkoutTime,
                COALESCE(CAST(att.status AS string), 'PENDING'),
                att.totalHours, att.notes, att.proofFileUrl
            )
            FROM Application app
            JOIN app.volunteer u
            LEFT JOIN Attendance att ON att.application.appId = app.appId
            WHERE app.opportunity.oppId = :opportunityId
            AND app.status IN (com.fptuni.vms.model.Application$ApplicationStatus.APPROVED, 
                               com.fptuni.vms.model.Application$ApplicationStatus.COMPLETED)
            ORDER BY u.fullName ASC
            """;

        TypedQuery<AttendanceRecordDTO> query = em.createQuery(jpql, AttendanceRecordDTO.class);
        query.setParameter("opportunityId", opportunityId);

        return query.getResultList();
    }

    @Override
    public List<AttendanceRecordDTO> findAttendanceRecords(Integer opportunityId, String keyword, String status, int page, int size) {
        String baseJpql = """
            SELECT new com.fptuni.vms.dto.response.AttendanceRecordDTO(
                app.appId, att.attId, u.userId, u.fullName, u.email, u.phone,
                u.address, u.avatarUrl, att.checkinTime, att.checkoutTime,
                COALESCE(CAST(att.status AS string), 'PENDING'),
                att.totalHours, att.notes, att.proofFileUrl
            )
            FROM Application app
            JOIN app.volunteer u
            LEFT JOIN Attendance att ON att.application.appId = app.appId
            """;

        StringBuilder whereClause = new StringBuilder();
        whereClause.append("""
            WHERE app.opportunity.oppId = :opportunityId
            AND app.status IN (com.fptuni.vms.model.Application$ApplicationStatus.APPROVED, 
                                com.fptuni.vms.model.Application$ApplicationStatus.COMPLETED)
            """);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("opportunityId", opportunityId);

        // Thêm điều kiện LỌC THEO KEYWORD (tên/email)
        if (StringUtils.hasText(keyword)) {
            whereClause.append(" AND (LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword) ");
            parameters.put("keyword", "%" + keyword.toLowerCase().trim() + "%");
        }

        // Thêm điều kiện LỌC THEO STATUS
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("PENDING".equalsIgnoreCase(status)) {
                whereClause.append(" AND att.status IS NULL ");
            } else {
                whereClause.append(" AND att.status = :status ");
                parameters.put("status", Attendance.AttendanceStatus.valueOf(status.toUpperCase()));
            }
        }

        String finalJpql = baseJpql + whereClause + " ORDER BY u.fullName ASC";
        TypedQuery<AttendanceRecordDTO> query = em.createQuery(finalJpql, AttendanceRecordDTO.class);
        parameters.forEach(query::setParameter);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);

        return query.getResultList();
    }


    @Override
    public long countAttendanceRecords(Integer opportunityId, String keyword, String status) {
        String fromAndJoinClause = """
            FROM Application app
            JOIN app.volunteer u
            LEFT JOIN Attendance att ON att.application.appId = app.appId
            """;

        // Phần WHERE động (sao chép logic y hệt)
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("""
            WHERE app.opportunity.oppId = :opportunityId
            AND app.status IN (com.fptuni.vms.model.Application$ApplicationStatus.APPROVED, 
                                com.fptuni.vms.model.Application$ApplicationStatus.COMPLETED)
            """);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("opportunityId", opportunityId);

        if (StringUtils.hasText(keyword)) {
            whereClause.append(" AND (LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword) ");
            parameters.put("keyword", "%" + keyword.toLowerCase().trim() + "%");
        }

        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("PENDING".equalsIgnoreCase(status)) {
                whereClause.append(" AND att.status IS NULL ");
            } else {
                whereClause.append(" AND att.status = :status ");
                parameters.put("status", Attendance.AttendanceStatus.valueOf(status.toUpperCase()));
            }
        }

        String countJpql = "SELECT COUNT(app) " + fromAndJoinClause + whereClause;
        TypedQuery<Long> query = em.createQuery(countJpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public Application findApplicationById(Integer id) {
        Application application = em.find(Application.class, id);
        return application;
    }

    @Override
    public Attendance findAttendanceByApplicationId(Integer applicationId) {
        String jpql = "SELECT a FROM Attendance a WHERE a.application.appId = :applicationId";
        TypedQuery<Attendance> query = em.createQuery(jpql, Attendance.class);
        query.setParameter("applicationId", applicationId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Attendance createAttendance(Attendance attendance) {
        if (attendance == null) {
            return null;
        }
        em.persist(attendance);
        em.flush();
        return attendance;
    }

    @Override
    public Attendance updateAttendance(Attendance attendance) {
        Attendance merged = em.merge(attendance);
        em.flush();
        return merged;
    }



}