package com.fptuni.vms.repository.impl;

import com.fptuni.vms.dto.response.OpportunitySummaryDto;
import com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto;
import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public class VolunteerRatingRepositoryImpl implements VolunteerRatingRepository {

    @PersistenceContext
    private EntityManager em;

    // ===================== 1. LIST OPPORTUNITIES =====================
    @Override
    public List<OpportunitySummaryDto> findOpportunitiesByOrg(
            int orgId, String keyword, String eventStatus, String sort, int offset, int limit) {

        LocalDateTime now = LocalDateTime.now();

        StringBuilder jpql = new StringBuilder("""
        SELECT new com.fptuni.vms.dto.response.OpportunitySummaryDto(
            o.oppId,
            o.title,
            o.location,
            o.startTime,
            o.endTime,

            (SELECT COUNT(a)
             FROM Application a
             WHERE a.opportunity.oppId = o.oppId
               AND a.status IN ('APPROVED','COMPLETED')
            ),

            (SELECT COUNT(vr)
             FROM VolunteerRating vr
             WHERE vr.opportunity.oppId = o.oppId
            ),

            (SELECT COUNT(a2)
             FROM Application a2
             LEFT JOIN Attendance att2 ON att2.application.appId = a2.appId
             WHERE a2.opportunity.oppId = o.oppId
               AND att2.checkinTime IS NOT NULL
               AND NOT EXISTS (
                   SELECT 1 FROM VolunteerRating vr2
                   WHERE vr2.opportunity.oppId = o.oppId
                     AND vr2.rateeUser.userId = a2.volunteer.userId
               )
            ),

            CASE
                WHEN o.startTime > :now THEN 'UPCOMING'
                WHEN o.endTime   < :now THEN 'FINISHED'
                ELSE 'ONGOING'
            END
        )
        FROM Opportunity o
        WHERE o.organization.orgId = :orgId
    """);

        // Filter keyword
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(o.title) LIKE LOWER(:kw) ");
        }

        // Filter eventStatus
        switch (eventStatus == null ? "ALL" : eventStatus.toUpperCase()) {
            case "UPCOMING" -> jpql.append(" AND o.startTime > :now ");
            case "ONGOING"  -> jpql.append(" AND o.startTime <= :now AND o.endTime >= :now ");
            case "FINISHED" -> jpql.append(" AND o.endTime < :now ");
            default -> { /* ALL - no filter */ }
        }

        // Sorting
        switch (sort == null ? "recent" : sort.toLowerCase()) {
            case "start" -> jpql.append(" ORDER BY o.startTime ASC ");
            case "end"   -> jpql.append(" ORDER BY o.endTime DESC ");
            case "name"  -> jpql.append(" ORDER BY o.title ASC ");
            default      -> jpql.append(" ORDER BY o.createdAt DESC ");
        }

        TypedQuery<OpportunitySummaryDto> query = em
                .createQuery(jpql.toString(), OpportunitySummaryDto.class)
                .setParameter("orgId", orgId)
                .setParameter("now", now)         // luôn set vì SELECT/CASE dùng :now
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("kw", "%" + keyword + "%");
        }

        return query.getResultList();
    }


    @Override
    public long countOpportunitiesByOrg(int orgId, String keyword, String eventStatus) {
        StringBuilder jpql = new StringBuilder("""
            SELECT COUNT(o)
            FROM Opportunity o
            WHERE o.organization.orgId = :orgId
        """);

        LocalDateTime now = LocalDateTime.now();

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(o.title) LIKE LOWER(:kw) ");
        }
        switch (eventStatus.toUpperCase()) {
            case "UPCOMING":
                jpql.append(" AND o.startTime > :now ");
                break;
            case "ONGOING":
                jpql.append(" AND o.startTime <= :now AND o.endTime >= :now ");
                break;
            case "FINISHED":
                jpql.append(" AND o.endTime < :now ");
                break;
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("orgId", orgId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("kw", "%" + keyword + "%");
        }
        if (!"ALL".equalsIgnoreCase(eventStatus)) {
            query.setParameter("now", now);
        }

        return query.getSingleResult();
    }

    // ===================== 2. LIST VOLUNTEERS FOR OPPORTUNITY =====================
    @Override
    public List<OpportunityVolunteerRatingDto> findVolunteersForOpportunity(
            int orgId, int opportunityId, String keyword, String statusFilter,
            String sort, int offset, int limit) {

        StringBuilder jpql = new StringBuilder("""
            SELECT new com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto(
                u.userId, u.fullName, u.avatarUrl,
                o.oppId, o.title, o.location, o.startTime, o.endTime,
                att.checkinTime, att.checkoutTime, att.totalHours,
                vr.id, vr.stars, vr.comment, vr.createdAt,
                CASE
                            WHEN att.checkinTime IS NULL THEN 'NOT_ATTENDED'
                            WHEN att.checkoutTime IS NULL THEN 'IN_PROGRESS'
                            WHEN vr.id IS NOT NULL THEN 'RATED'
                            ELSE 'PENDING'
                        END
            )
            FROM Application a
            JOIN a.volunteer u
            JOIN a.opportunity o
            LEFT JOIN Attendance att ON att.application.appId = a.appId
            LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = u.userId
            WHERE o.organization.orgId = :orgId AND o.oppId = :opportunityId
        """);

        // Filter keyword
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(u.fullName) LIKE LOWER(:kw) ");
        }

        // Filter by volunteer rating status
        switch (statusFilter.toUpperCase()) {
            case "NOT_ATTENDED":
                jpql.append(" AND att.checkinTime IS NULL ");
                break;
            case "PENDING":
                jpql.append(" AND att.checkinTime IS NOT NULL AND vr.id IS NULL ");
                break;
            case "RATED":
                jpql.append(" AND vr.id IS NOT NULL ");
                break;
            default: // ALL
                break;
        }

        // Sorting
        switch (sort) {
            case "nameAsc" -> jpql.append(" ORDER BY u.fullName ASC ");
            case "nameDesc" -> jpql.append(" ORDER BY u.fullName DESC ");
            case "hoursAsc" -> jpql.append(" ORDER BY att.totalHours ASC ");
            case "hoursDesc" -> jpql.append(" ORDER BY att.totalHours DESC ");
            case "checkinAsc" -> jpql.append(" ORDER BY att.checkinTime ASC ");
            case "checkinDesc" -> jpql.append(" ORDER BY att.checkinTime DESC ");
            default -> jpql.append(" ORDER BY u.fullName ASC ");
        }


        TypedQuery<OpportunityVolunteerRatingDto> query =
                em.createQuery(jpql.toString(), OpportunityVolunteerRatingDto.class)
                        .setParameter("orgId", orgId)
                        .setParameter("opportunityId", opportunityId)
                        .setFirstResult(offset)
                        .setMaxResults(limit);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("kw", "%" + keyword + "%");
        }

        return query.getResultList();
    }

    @Override
    public long countVolunteersForOpportunity(int orgId, int opportunityId, String keyword, String statusFilter) {
        StringBuilder jpql = new StringBuilder("""
            SELECT COUNT(a)
            FROM Application a
            JOIN a.opportunity o
            LEFT JOIN Attendance att ON att.application.appId = a.appId
            LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = a.volunteer.userId
            WHERE o.organization.orgId = :orgId AND o.oppId = :opportunityId
        """);

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(a.volunteer.fullName) LIKE LOWER(:kw) ");
        }
        switch (statusFilter.toUpperCase()) {
            case "NOT_ATTENDED":
                jpql.append(" AND att.checkinTime IS NULL ");
                break;
            case "PENDING":
                jpql.append(" AND att.checkinTime IS NOT NULL AND vr.id IS NULL ");
                break;
            case "RATED":
                jpql.append(" AND vr.id IS NOT NULL ");
                break;
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("orgId", orgId)
                .setParameter("opportunityId", opportunityId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("kw", "%" + keyword + "%");
        }

        return query.getSingleResult();
    }

    // ===================== 3. CRUD =====================
    @Override
    public VolunteerRating findById(int id) {
        return em.find(VolunteerRating.class, id);
    }

    @Override
    public void save(VolunteerRating rating) {
        em.persist(rating);
    }

    @Override
    public void update(VolunteerRating rating) {
        em.merge(rating);
    }

    // ===================== 4. BADGE =====================
    @Override
    public Double getAverageStarsByUser(int userId) {
        String jpql = """
        SELECT AVG(vr.stars)
        FROM VolunteerRating vr
        WHERE vr.rateeUser.userId = :userId
    """;
        Double avg = em.createQuery(jpql, Double.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return avg != null ? avg : 0.0;
    }
    @Override
    public long countPendingAll(int orgId) {
        String jpql = """
            SELECT COUNT(a)
            FROM Application a
            JOIN a.opportunity o
            LEFT JOIN Attendance att ON att.application.appId = a.appId
            LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = a.volunteer.userId
            WHERE o.organization.orgId = :orgId
              AND att.checkinTime IS NOT NULL AND vr.id IS NULL
        """;
        return em.createQuery(jpql, Long.class)
                .setParameter("orgId", orgId)
                .getSingleResult();
    }

    @Override
    public boolean hasCheckedIn(int oppId, int userId) {
        String jpql = """
        SELECT COUNT(att)
        FROM Attendance att
        JOIN att.application a
        WHERE a.opportunity.oppId = :oppId
          AND a.volunteer.userId = :userId
          AND att.checkinTime IS NOT NULL
    """;
        Long count = em.createQuery(jpql, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public boolean hasRated(int oppId, int userId, int orgId) {
        String jpql = """
        SELECT COUNT(vr)
        FROM VolunteerRating vr
        WHERE vr.opportunity.oppId = :oppId
          AND vr.rateeUser.userId = :userId
          AND vr.raterOrg.orgId = :orgId
    """;
        Long count = em.createQuery(jpql, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("userId", userId)
                .setParameter("orgId", orgId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public long countRatedAll(int orgId) {
        String jpql = """
            SELECT COUNT(vr)
            FROM VolunteerRating vr
            JOIN vr.opportunity o
            WHERE o.organization.orgId = :orgId
        """;
        return em.createQuery(jpql, Long.class)
                .setParameter("orgId", orgId)
                .getSingleResult();
    }
}
