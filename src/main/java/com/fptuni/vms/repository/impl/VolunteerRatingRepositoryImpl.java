package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class VolunteerRatingRepositoryImpl implements VolunteerRatingRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<VolunteerRating> findByOrganization(int orgId, String keyword, Short stars, int offset, int limit) {
        String jpql = """
            SELECT vr FROM VolunteerRating vr
            JOIN vr.opportunity o
            JOIN vr.rateeUser u
            WHERE o.organization.orgId = :orgId
        """;
        if (keyword != null && !keyword.isBlank())
            jpql += " AND (LOWER(u.fullName) LIKE LOWER(:kw) OR LOWER(u.email) LIKE LOWER(:kw) OR LOWER(o.title) LIKE LOWER(:kw))";
        if (stars != null)
            jpql += " AND vr.stars = :stars";
        jpql += " ORDER BY vr.createdAt DESC";

        TypedQuery<VolunteerRating> q = em.createQuery(jpql, VolunteerRating.class)
                .setParameter("orgId", orgId)
                .setFirstResult(offset)
                .setMaxResults(limit);
        if (keyword != null && !keyword.isBlank())
            q.setParameter("kw", "%" + keyword + "%");
        if (stars != null)
            q.setParameter("stars", stars);
        return q.getResultList();
    }

    @Override
    public long countByOrganization(int orgId, String keyword, Short stars) {
        String jpql = """
            SELECT COUNT(vr) FROM VolunteerRating vr
            JOIN vr.opportunity o
            JOIN vr.rateeUser u
            WHERE o.organization.orgId = :orgId
        """;
        if (keyword != null && !keyword.isBlank())
            jpql += " AND (LOWER(u.fullName) LIKE LOWER(:kw) OR LOWER(u.email) LIKE LOWER(:kw) OR LOWER(o.title) LIKE LOWER(:kw))";
        if (stars != null)
            jpql += " AND vr.stars = :stars";

        TypedQuery<Long> q = em.createQuery(jpql, Long.class).setParameter("orgId", orgId);
        if (keyword != null && !keyword.isBlank())
            q.setParameter("kw", "%" + keyword + "%");
        if (stars != null)
            q.setParameter("stars", stars);
        return q.getSingleResult();
    }

    @Override
    public long countPending(int orgId) {
        String jpql = """
            SELECT COUNT(a)
            FROM Attendance a
            JOIN a.application app
            JOIN app.opportunity o
            WHERE o.organization.orgId = :orgId
              AND a.status IN ('PRESENT','COMPLETED')
              AND NOT EXISTS (
                  SELECT 1 FROM VolunteerRating vr
                  WHERE vr.opportunity.oppId = o.oppId
                    AND vr.rateeUser.userId = app.volunteer.userId
              )
        """;
        return em.createQuery(jpql, Long.class)
                .setParameter("orgId", orgId)
                .getSingleResult();
    }

    @Override
    public long countDone(int orgId) {
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
}