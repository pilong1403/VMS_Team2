package com.fptuni.vms.repository.impl;

import com.fptuni.vms.dto.EventHistoryDto;
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
                WHEN o.status = 'CANCELLED' THEN 'CANCELLED'
                WHEN o.startTime > :now THEN 'UPCOMING'
                WHEN o.endTime   < :now THEN 'FINISHED'
                ELSE 'ONGOING'
            END
        )
        FROM Opportunity o
        WHERE o.organization.orgId = :orgId
          AND o.status <> 'DRAFT'
    """);

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(o.title) LIKE LOWER(:kw) ");
        }

        String es = (eventStatus == null ? "ALL" : eventStatus).toUpperCase();
        switch (es) {
            case "UPCOMING"  -> jpql.append(" AND o.status <> 'CANCELLED' AND o.startTime > :now ");
            case "ONGOING"   -> jpql.append(" AND o.status <> 'CANCELLED' AND o.startTime <= :now AND o.endTime >= :now ");
            case "FINISHED"  -> jpql.append(" AND o.status <> 'CANCELLED' AND o.endTime < :now ");
            case "CANCELLED" -> jpql.append(" AND o.status = 'CANCELLED' ");
            default -> { /* ALL */ }
        }

        switch (sort == null ? "recent" : sort.toLowerCase()) {
            case "start"        -> jpql.append(" ORDER BY o.startTime ASC ");
            case "end"          -> jpql.append(" ORDER BY o.endTime DESC ");
            case "name"         -> jpql.append(" ORDER BY o.title ASC ");
            case "participants" -> jpql.append(" ORDER BY 6 DESC "); // participantCount
            default             -> jpql.append(" ORDER BY o.createdAt DESC ");
        }

        var q = em.createQuery(jpql.toString(), OpportunitySummaryDto.class)
                .setParameter("orgId", orgId)
                .setParameter("now", now)
                .setFirstResult(offset)
                .setMaxResults(limit);
        if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword + "%");
        return q.getResultList();
    }

    @Override
    public long countOpportunitiesByOrg(int orgId, String keyword, String eventStatus) {
        StringBuilder jpql = new StringBuilder("""
        SELECT COUNT(o)
        FROM Opportunity o
        WHERE o.organization.orgId = :orgId
          AND o.status <> 'DRAFT'
    """);

        LocalDateTime now = LocalDateTime.now();

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(o.title) LIKE LOWER(:kw) ");
        }

        String es = (eventStatus == null ? "ALL" : eventStatus).toUpperCase();
        switch (es) {
            case "UPCOMING"  -> jpql.append(" AND o.status <> 'CANCELLED' AND o.startTime > :now ");
            case "ONGOING"   -> jpql.append(" AND o.status <> 'CANCELLED' AND o.startTime <= :now AND o.endTime >= :now ");
            case "FINISHED"  -> jpql.append(" AND o.status <> 'CANCELLED' AND o.endTime < :now ");
            case "CANCELLED" -> jpql.append(" AND o.status = 'CANCELLED' ");
            default -> { /* ALL */ }
        }

        var q = em.createQuery(jpql.toString(), Long.class)
                .setParameter("orgId", orgId);
        if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword + "%");
        if (!"ALL".equals(es) && !"CANCELLED".equals(es)) q.setParameter("now", now);
        return q.getSingleResult();
    }

    // ===================== 2. LIST VOLUNTEERS FOR OPPORTUNITY
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
                                WHEN vr.id IS NOT NULL THEN 'RATED'
                                WHEN att.checkinTime IS NULL THEN 'NOT_ATTENDED'
                                WHEN att.checkoutTime IS NULL THEN 'IN_PROGRESS'
                                ELSE 'PENDING'
                            END
                        )
                        FROM Application a
                        JOIN a.volunteer u
                        JOIN a.opportunity o
                        LEFT JOIN Attendance att ON att.application.appId = a.appId
                        LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = u.userId
                        WHERE o.organization.orgId = :orgId
                          AND o.oppId = :opportunityId
                          AND a.status IN ('APPROVED','COMPLETED')
                    """);

        // Filter keyword
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND LOWER(u.fullName) LIKE LOWER(:kw) ");
        }

        // Filter by volunteer rating status
        switch (statusFilter.toUpperCase()) {
            case "NOT_ATTENDED":
                // Chưa điểm danh: chưa checkin
                jpql.append(" AND att.checkinTime IS NULL ");
                break;

            case "IN_PROGRESS":
                // Đang tham gia: đã checkin nhưng chưa checkout
                jpql.append(" AND att.checkinTime IS NOT NULL AND att.checkoutTime IS NULL ");
                break;

            case "PENDING":
                // Chờ đánh giá: đã checkout đầy đủ giờ nhưng chưa rating
                jpql.append(" AND att.checkinTime IS NOT NULL AND att.checkoutTime IS NOT NULL AND vr.id IS NULL ");
                break;

            case "RATED":
                // Đã đánh giá: phải đảm bảo có rating và đã tham gia đầy đủ
                jpql.append(" AND vr.id IS NOT NULL AND att.checkinTime IS NOT NULL AND att.checkoutTime IS NOT NULL ");
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

        TypedQuery<OpportunityVolunteerRatingDto> query = em
                .createQuery(jpql.toString(), OpportunityVolunteerRatingDto.class)
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
                    WHERE o.organization.orgId = :orgId
                      AND o.oppId = :opportunityId
                      AND a.status IN ('APPROVED','COMPLETED')
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

    // ===================== 5. VOLUNTEER EVENT HISTORY =====================
    @Override
    public List<EventHistoryDto> findVolunteerEventHistory(int volunteerId, int offset, int limit) {
        String jpql = """
                    SELECT new com.fptuni.vms.dto.EventHistoryDto(
                        a.appId,
                        o.oppId,
                        o.title,
                        org.name,
                        o.location,
                        o.startTime,
                        o.endTime,
                        o.thumbnailUrl,
                        o.subtitle,
                        CASE WHEN att.checkinTime IS NOT NULL THEN true ELSE false END,
                        CASE WHEN vr.id IS NOT NULL THEN true ELSE false END,
                        vr.id,
                        vr.stars,
                        vr.comment
                    )
                    FROM Application a
                    JOIN a.opportunity o
                    JOIN o.organization org
                    LEFT JOIN Attendance att ON att.application.appId = a.appId
                    LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = :volunteerId
                    WHERE a.volunteer.userId = :volunteerId
                      AND a.status IN ('APPROVED', 'COMPLETED')
                      AND o.endTime < CURRENT_TIMESTAMP
                    ORDER BY o.endTime DESC
                """;

        return em.createQuery(jpql, EventHistoryDto.class)
                .setParameter("volunteerId", volunteerId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long countVolunteerEventHistory(int volunteerId) {
        String jpql = """
                    SELECT COUNT(a)
                    FROM Application a
                    JOIN a.opportunity o
                    WHERE a.volunteer.userId = :volunteerId
                      AND a.status IN ('APPROVED', 'COMPLETED')
                      AND o.endTime < CURRENT_TIMESTAMP
                """;

        return em.createQuery(jpql, Long.class)
                .setParameter("volunteerId", volunteerId)
                .getSingleResult();
    }

    // ===================== 6. VOLUNTEER RATING CHECKS =====================
    @Override
    public boolean canVolunteerRate(int oppId, int volunteerId) {
        // Check if volunteer has attended and event is completed, and hasn't rated yet
        String jpql = """
                    SELECT COUNT(a)
                    FROM Application a
                    JOIN a.opportunity o
                    LEFT JOIN Attendance att ON att.application.appId = a.appId
                    LEFT JOIN VolunteerRating vr ON vr.opportunity.oppId = o.oppId AND vr.rateeUser.userId = :volunteerId
                    WHERE a.volunteer.userId = :volunteerId
                      AND o.oppId = :oppId
                      AND a.status IN ('APPROVED', 'COMPLETED')
                      AND o.endTime < CURRENT_TIMESTAMP
                      AND att.checkinTime IS NOT NULL
                      AND vr.id IS NULL
                """;

        Long count = em.createQuery(jpql, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("volunteerId", volunteerId)
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public VolunteerRating findVolunteerRating(int oppId, int volunteerId) {
        String jpql = """
                    SELECT vr
                    FROM VolunteerRating vr
                    WHERE vr.opportunity.oppId = :oppId
                      AND vr.rateeUser.userId = :volunteerId
                """;

        List<VolunteerRating> results = em.createQuery(jpql, VolunteerRating.class)
                .setParameter("oppId", oppId)
                .setParameter("volunteerId", volunteerId)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }
}
