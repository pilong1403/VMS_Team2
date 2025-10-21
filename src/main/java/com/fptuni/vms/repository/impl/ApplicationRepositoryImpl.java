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

    @PersistenceContext
    private EntityManager em;

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
                em.persist(application);
                return application;
            } else {
                return em.merge(application);
            }
        } catch (PersistenceException e) {
            throw e; // cho service handle lỗi unique constraint
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
        // Đếm các đơn PENDING/APPROVED/COMPLETED (không tính REJECTED/CANCELLED)
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
        // fetch join o & org để hiển thị tên tổ chức, tiêu đề... không bị N+1
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

    // ================== PhiLong iter2 query theo tổ chức ==================
    @Override
    public List<Application> findOrgApplications(Integer orgId, String q,
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
    public long countOrgApplications(Integer orgId, String q,
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
    public Map<Application.ApplicationStatus, Long> computeOrgAppStats(Integer orgId) {
        List<Object[]> rows = em.createQuery("""
                SELECT a.status, COUNT(a.appId)
                  FROM Application a
                  JOIN a.opportunity o
                  JOIN o.organization org
                 WHERE org.orgId = :orgId
                 GROUP BY a.status
                """, Object[].class)
                .setParameter("orgId", orgId)
                .getResultList();

        Map<Application.ApplicationStatus, Long> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put((Application.ApplicationStatus) r[0], (Long) r[1]);
        }
        return m;
    }
}
