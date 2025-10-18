// src/main/java/com/fptuni/vms/repository/impl/OtpVerificationRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.OtpVerification;
import com.fptuni.vms.model.OtpVerification.Purpose;
import com.fptuni.vms.repository.OtpVerificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OtpVerificationRepositoryImpl implements OtpVerificationRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<OtpVerification> findTop1ByEmailAndPurposeOrderByCreatedAtDesc(String email, Purpose purpose) {
        if (email == null || email.isBlank()) return Optional.empty();
        TypedQuery<OtpVerification> q = em.createQuery(
                "SELECT v FROM OtpVerification v " +
                        "WHERE v.email = :email AND v.purpose = :p " +
                        "ORDER BY v.createdAt DESC", OtpVerification.class);
        q.setParameter("email", email);
        q.setParameter("p", purpose);
        q.setMaxResults(1);
        List<OtpVerification> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public OtpVerification save(OtpVerification v) {
        if (v == null) return null;
        if (v.getOtpId() == null || v.getOtpId() == 0) {
            em.persist(v);
            return v;
        } else {
            return em.merge(v);
        }
    }

    @Override
    public int countActiveByEmailAndPurpose(String email, Purpose purpose) {
        LocalDateTime now = LocalDateTime.now(); // đồng bộ timezone với entity của bạn
        Long cnt = em.createQuery(
                        "SELECT COUNT(v) FROM OtpVerification v " +
                                "WHERE v.email = :email AND v.purpose = :p " +
                                "AND v.verified = false AND v.expiredAt > :now", Long.class)
                .setParameter("email", email)
                .setParameter("p", purpose)
                .setParameter("now", now)
                .getSingleResult();
        return cnt == null ? 0 : cnt.intValue();
    }

    @Override
    public int invalidateActiveByEmailAndPurpose(String email, Purpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        return em.createQuery(
                        "UPDATE OtpVerification v SET v.verified = true, v.consumedAt = :now " +
                                "WHERE v.email = :email AND v.purpose = :p " +
                                "AND v.verified = false AND v.expiredAt > :now")
                .setParameter("now", now)
                .setParameter("email", email)
                .setParameter("p", purpose)
                .executeUpdate();
    }
}
