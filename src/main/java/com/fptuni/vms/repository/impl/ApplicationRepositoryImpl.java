package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
@Transactional
public class ApplicationRepositoryImpl implements ApplicationRepository {

    @PersistenceContext // Inject EntityManager
    private EntityManager em;

    // Kiểm tra volunteer đã apply chưa
    @Override
    public boolean existsByOppIdAndVolunteerId(Integer oppId, Integer volunteerId) {
        try {
            em.createQuery("""
                    SELECT a.appId FROM Application a
                    WHERE a.opportunity.oppId = :oppId
                      AND a.volunteer.userId = :uid
                    """, Integer.class)
                    .setParameter("oppId", oppId)
                    .setParameter("uid", volunteerId)
                    .setMaxResults(1)
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public Application save(Application application) {
        try {
            if (application.getAppId() == null) {
                em.persist(application); // insert new application
                return application;
            } else {
                return em.merge(application); // update existing application
            }
        } catch (PersistenceException e) {
            throw e;
        }
    }

    @Override
    public Opportunity findOpportunityById(Integer oppId) {
        return em.find(Opportunity.class, oppId);
    }

    @Override
    public User findUserById(Integer userId) {
        return em.find(User.class, userId);
    }

    @Override
    public long countByOppId(Integer oppId) {
        Long cnt = em.createQuery("""
                SELECT COUNT(a.appId)
                FROM Application a
                WHERE a.opportunity.oppId = :oppId
                  AND a.status IN (:s1, :s2, :s3)
                """, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("s1", Application.ApplicationStatus.PENDING)
                .setParameter("s2", Application.ApplicationStatus.APPROVED)
                .setParameter("s3", Application.ApplicationStatus.COMPLETED)
                .getSingleResult();
        return cnt == null ? 0L : cnt;
    }

    @Override
    public User saveUser(User user) {
        if (user.getUserId() == null)
            throw new IllegalArgumentException("Missing userId");
        return em.merge(user);
    }

    @Override
    public List<Application> findAllByVolunteerId(Integer volunteerId) {
        return em.createQuery("""
                SELECT a
                FROM Application a
                JOIN FETCH a.opportunity o
                JOIN FETCH o.organization org
                WHERE a.volunteer.userId = :uid
                ORDER BY a.appliedAt DESC
                """, Application.class)
                .setParameter("uid", volunteerId)
                .getResultList();
    }

    // query cho volunteer có tìm kiếm/loc/sort PhiLong
    @Override
    public List<Application> findMyApplications(Integer volunteerId,
            Application.ApplicationStatus status,
            String q,
            String sortDir,
            int offset,
            int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT a
                  FROM Application a
                  JOIN FETCH a.opportunity o
                  JOIN FETCH o.organization org
                 WHERE a.volunteer.userId = :uid
                """);
        if (status != null) {
            jpql.append(" AND a.status = :status");
        }
        if (q != null && !q.isBlank()) {
            jpql.append("""
                         AND (LOWER(org.name) LIKE :kw
                           OR LOWER(o.title)  LIKE :kw)
                    """);
        }
        // sort theo appliedAt
        jpql.append(" ORDER BY a.appliedAt ").append(("ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC"));

        var query = em.createQuery(jpql.toString(), Application.class)
                .setParameter("uid", volunteerId)
                .setFirstResult(Math.max(offset, 0))
                .setMaxResults(Math.max(limit, 1));

        if (status != null)
            query.setParameter("status", status);
        if (q != null && !q.isBlank())
            query.setParameter("kw", "%" + q.toLowerCase().trim() + "%");

        return query.getResultList();
    }

    @Override
    public long countMyApplications(Integer volunteerId,
            Application.ApplicationStatus status,
            String q) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(a.appId)
                  FROM Application a
                  JOIN a.opportunity o
                  JOIN o.organization org
                 WHERE a.volunteer.userId = :uid
                """);
        if (status != null) {
            jpql.append(" AND a.status = :status");
        }
        if (q != null && !q.isBlank()) {
            jpql.append("""
                         AND (LOWER(org.name) LIKE :kw
                           OR LOWER(o.title)  LIKE :kw)
                    """);
        }

        var query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("uid", volunteerId);

        if (status != null)
            query.setParameter("status", status);
        if (q != null && !q.isBlank())
            query.setParameter("kw", "%" + q.toLowerCase().trim() + "%");

        Long total = query.getSingleResult();
        return total == null ? 0L : total;
    }

    @Override
    public List<Application> findOrgApplications(Integer orgId, Integer oppId, String q,
            Application.ApplicationStatus status,
            LocalDateTime from, LocalDateTime to,
            int offset, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT a
                  FROM Application a
                  JOIN FETCH a.volunteer v
                  JOIN FETCH a.opportunity o
                  JOIN FETCH o.organization org
                 WHERE org.orgId = :orgId
                """);
        if (oppId != null)
            jpql.append(" AND o.oppId = :oppId");
        if (status != null)
            jpql.append(" AND a.status = :status");
        if (q != null && !q.isBlank()) {
            jpql.append("""
                       AND (LOWER(v.fullName) LIKE :kw
                         OR LOWER(o.title)     LIKE :kw)
                    """);
        }
        if (from != null)
            jpql.append(" AND a.appliedAt >= :from");
        if (to != null)
            jpql.append(" AND a.appliedAt <  :to");
        jpql.append(" ORDER BY a.appliedAt DESC");

        var query = em.createQuery(jpql.toString(), Application.class)
                .setParameter("orgId", orgId)
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (oppId != null)
            query.setParameter("oppId", oppId);
        if (status != null)
            query.setParameter("status", status);
        if (q != null && !q.isBlank())
            query.setParameter("kw", "%" + q.toLowerCase().trim() + "%");
        if (from != null)
            query.setParameter("from", from);
        if (to != null)
            query.setParameter("to", to);

        return query.getResultList();
    }

    @Override
    public long countOrgApplications(Integer orgId, Integer oppId, String q,
            Application.ApplicationStatus status,
            LocalDateTime from, LocalDateTime to) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(a.appId)
                  FROM Application a
                  JOIN a.volunteer v
                  JOIN a.opportunity o
                  JOIN o.organization org
                 WHERE org.orgId = :orgId
                """);
        if (oppId != null)
            jpql.append(" AND o.oppId = :oppId");
        if (status != null)
            jpql.append(" AND a.status = :status");
        if (q != null && !q.isBlank()) {
            jpql.append("""
                       AND (LOWER(v.fullName) LIKE :kw
                         OR LOWER(o.title)     LIKE :kw)
                    """);
        }
        if (from != null)
            jpql.append(" AND a.appliedAt >= :from");
        if (to != null)
            jpql.append(" AND a.appliedAt <  :to");

        var query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("orgId", orgId);

        if (oppId != null)
            query.setParameter("oppId", oppId);
        if (status != null)
            query.setParameter("status", status);
        if (q != null && !q.isBlank())
            query.setParameter("kw", "%" + q.toLowerCase().trim() + "%");
        if (from != null)
            query.setParameter("from", from);
        if (to != null)
            query.setParameter("to", to);

        Long total = query.getSingleResult();
        return total == null ? 0L : total;
    }

    @Override
    public Map<Application.ApplicationStatus, Long> computeOrgAppStats(Integer orgId,
            Integer oppId, String q, Application.ApplicationStatus status,
            LocalDateTime from, LocalDateTime to) {
        StringBuilder jpql = new StringBuilder("""
                SELECT a.status, COUNT(a.appId)
                  FROM Application a
                  JOIN a.opportunity o
                  JOIN o.organization org
                 WHERE org.orgId = :orgId
                """);
        if (oppId != null)
            jpql.append(" AND o.oppId = :oppId");
        if (status != null)
            jpql.append(" AND a.status = :status");
        if (q != null && !q.isBlank()) {
            jpql.append("""
                       AND (LOWER(a.volunteer.fullName) LIKE :kw
                         OR LOWER(o.title)              LIKE :kw)
                    """);
        }
        if (from != null)
            jpql.append(" AND a.appliedAt >= :from");
        if (to != null)
            jpql.append(" AND a.appliedAt <  :to");
        jpql.append(" GROUP BY a.status");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("orgId", orgId);

        if (oppId != null)
            query.setParameter("oppId", oppId);
        if (status != null)
            query.setParameter("status", status);
        if (q != null && !q.isBlank())
            query.setParameter("kw", "%" + q.toLowerCase().trim() + "%");
        if (from != null)
            query.setParameter("from", from);
        if (to != null)
            query.setParameter("to", to);

        List<Object[]> rows = query.getResultList();
        Map<Application.ApplicationStatus, Long> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put((Application.ApplicationStatus) r[0], (Long) r[1]);
        }
        return m;
    }

    @Override
    public Application findByIdAndOrgId(Integer appId, Integer orgId) {
        try {
            return em.createQuery("""
                    SELECT a
                      FROM Application a
                      JOIN FETCH a.volunteer v
                      JOIN FETCH a.opportunity o
                      JOIN FETCH o.organization org
                     WHERE a.appId = :appId
                       AND org.orgId = :orgId
                    """, Application.class)
                    .setParameter("appId", appId)
                    .setParameter("orgId", orgId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<User> findApprovedVolunteersByOppId(Integer oppId) {
        return em.createQuery("""
                SELECT v
                  FROM Application a
                  JOIN a.volunteer v
                 WHERE a.opportunity.oppId = :oppId
                   AND a.status IN (:s1, :s2)
                 ORDER BY a.appliedAt DESC
                """, User.class)
                .setParameter("oppId", oppId)
                .setParameter("s1", Application.ApplicationStatus.APPROVED)
                .setParameter("s2", Application.ApplicationStatus.COMPLETED)
                .getResultList();
    }

    @Override
    public List<Application> findApprovedApplicationsByOppId(Integer oppId) {
        return em.createQuery("""
                SELECT a
                  FROM Application a
                  JOIN FETCH a.volunteer v
                  JOIN FETCH a.opportunity o
                  JOIN FETCH o.organization org
                 WHERE a.opportunity.oppId = :oppId
                   AND a.status IN (:s1, :s2)
                 ORDER BY a.appliedAt DESC
                """, Application.class)
                .setParameter("oppId", oppId)
                .setParameter("s1", Application.ApplicationStatus.APPROVED)
                .setParameter("s2", Application.ApplicationStatus.COMPLETED)
                .getResultList();
    }

    // Đếm số đơn PENDING/APPROVED/COMPLETED theo oppId
    @Override
    public long countApprovedByOppId(Integer oppId) {
        if (oppId == null)
            return 0L;

        return em.createQuery("""
                SELECT COUNT(a)
                FROM Application a
                WHERE a.opportunity.oppId = :id
                  AND a.status IN (:p, :a, :c)
                """, Long.class)
                .setParameter("id", oppId)
                .setParameter("p", Application.ApplicationStatus.PENDING)
                .setParameter("a", Application.ApplicationStatus.APPROVED)
                .setParameter("c", Application.ApplicationStatus.COMPLETED)
                .getSingleResult();
    }

    // ====== check trùng thời gian với các đơn đang PENDING/APPROVED ======
    @Override
    public boolean hasOverlappingActiveApplications(Integer volunteerId,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            Integer excludeOppId) {
        if (volunteerId == null || newStart == null || newEnd == null)
            return false;

        String jpql = """
                SELECT COUNT(a.appId)
                  FROM Application a
                  JOIN a.opportunity o
                 WHERE a.volunteer.userId = :uid
                   AND a.status IN (:s1, :s2)        /* PENDING, APPROVED */
                   AND (:excludeId IS NULL OR o.oppId <> :excludeId)
                   AND o.startTime IS NOT NULL
                   AND o.endTime   IS NOT NULL
                   AND o.startTime < :newEnd         /* overlap core */
                   AND o.endTime   > :newStart
                """;

        Long cnt = em.createQuery(jpql, Long.class)
                .setParameter("uid", volunteerId)
                .setParameter("s1", Application.ApplicationStatus.PENDING)
                .setParameter("s2", Application.ApplicationStatus.APPROVED)
                .setParameter("excludeId", excludeOppId)
                .setParameter("newStart", newStart)
                .setParameter("newEnd", newEnd)
                .getSingleResult();

        return cnt != null && cnt > 0;
    }

    @Override
    public Application findByIdAndVolunteerId(Integer appId, Integer volunteerId) {
        try {
            return em.createQuery("""
                    SELECT a
                      FROM Application a
                      JOIN FETCH a.opportunity o
                      JOIN FETCH o.organization org
                     WHERE a.appId = :appId
                       AND a.volunteer.userId = :uid
                    """, Application.class)
                    .setParameter("appId", appId)
                    .setParameter("uid", volunteerId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public long countApprovedApplications(Integer oppId) {
        Long cnt = em.createQuery("""
                SELECT COUNT(a.appId)
                FROM Application a
                WHERE a.opportunity.oppId = :oppId
                  AND a.status IN (:s1, :s2)
                """, Long.class)
                .setParameter("oppId", oppId)
                .setParameter("s1", Application.ApplicationStatus.APPROVED)
                .setParameter("s2", Application.ApplicationStatus.COMPLETED)
                .getSingleResult();
        return cnt == null ? 0L : cnt;
    }

}
