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

import java.util.List;

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
}
