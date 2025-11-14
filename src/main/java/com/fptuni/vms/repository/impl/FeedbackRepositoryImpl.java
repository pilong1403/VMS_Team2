package com.fptuni.vms.repository.impl;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.model.Feedback;
import com.fptuni.vms.repository.FeedbackRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class FeedbackRepositoryImpl implements FeedbackRepository {

    @PersistenceContext
    private EntityManager em;

    // ===================== 1. VOLUNTEER EVENT HISTORY =====================
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
                        o.category.categoryId,
                        CASE WHEN att.checkinTime IS NOT NULL THEN true ELSE false END,
                        CASE WHEN fb.feedbackId IS NOT NULL THEN true ELSE false END,
                        fb.feedbackId,
                        fb.rating,
                        fb.content,
                        CASE WHEN vr.id IS NOT NULL THEN true ELSE false END,
                        vr.stars,
                        vr.comment
                    )
                    FROM Application a
                    JOIN a.opportunity o
                    JOIN o.organization org
                    LEFT JOIN Attendance att ON att.application.appId = a.appId
                    LEFT JOIN Feedback fb ON fb.opportunity.oppId = o.oppId AND fb.user.userId = :volunteerId AND fb.feedbackType = 'VOLUNTEER'
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

    // ===================== 2. VOLUNTEER FEEDBACK CHECKS =====================
    @Override
    public boolean canVolunteerGiveFeedback(int oppId, int volunteerId) {
        // Check if volunteer has attended and event is completed, and hasn't given
        // feedback yet
        String jpql = """
                    SELECT COUNT(a)
                    FROM Application a
                    JOIN a.opportunity o
                    LEFT JOIN Attendance att ON att.application.appId = a.appId
                    LEFT JOIN Feedback fb ON fb.opportunity.oppId = o.oppId AND fb.user.userId = :volunteerId AND fb.feedbackType = 'VOLUNTEER'
                    WHERE a.volunteer.userId = :volunteerId
                      AND o.oppId = :oppId
                      AND a.status IN ('APPROVED', 'COMPLETED')
                      AND o.endTime < CURRENT_TIMESTAMP
                      AND att.checkinTime IS NOT NULL
                      AND fb.feedbackId IS NULL
                """;

        Long count = em.createQuery(jpql, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("volunteerId", volunteerId)
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public Feedback findVolunteerFeedback(int oppId, int volunteerId) {
        String jpql = """
                    SELECT fb
                    FROM Feedback fb
                    WHERE fb.opportunity.oppId = :oppId
                      AND fb.user.userId = :volunteerId
                      AND fb.feedbackType = 'VOLUNTEER'
                """;

        List<Feedback> results = em.createQuery(jpql, Feedback.class)
                .setParameter("oppId", oppId)
                .setParameter("volunteerId", volunteerId)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Feedback> findByOpportunity(int oppId) {
        String jpql = """
                SELECT fb
                FROM Feedback fb
                JOIN FETCH fb.user u
                WHERE fb.opportunity.oppId = :oppId
                  AND fb.feedbackType = 'VOLUNTEER'
                ORDER BY fb.createdAt DESC
                """;
        return em.createQuery(jpql, Feedback.class)
                .setParameter("oppId", oppId)
                .getResultList();
    }

    // ===================== 3. CRUD =====================
    @Override
    public Feedback findById(int id) {
        return em.find(Feedback.class, id);
    }

    @Override
    public void save(Feedback feedback) {
        em.persist(feedback);
    }

    @Override
    public void update(Feedback feedback) {
        em.merge(feedback);
    }

    // ===================== 4. STATISTICS =====================
    @Override
    public long countAll() {
        String jpql = "SELECT COUNT(fb) FROM Feedback fb";
        return em.createQuery(jpql, Long.class).getSingleResult();
    }
}
