// src/main/java/com/fptuni/vms/repository/impl/OrganizationRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {

    @PersistenceContext
    private EntityManager em; // container-managed, transaction-scoped

    @Override
    public Optional<Organization> findByOwnerId(Integer ownerId) {
        if (ownerId == null) return Optional.empty();

        // Lấy bản ghi theo owner_id (tối đa 1)
        TypedQuery<Organization> q = em.createQuery(
                "SELECT o FROM Organization o WHERE o.owner.userId = :ownerId ORDER BY o.createdAt DESC",
                Organization.class
        );
        q.setParameter("ownerId", ownerId);
        q.setMaxResults(1);
        List<Organization> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public boolean existsByOwner(User owner) {
        if (owner == null || owner.getUserId() == null) return false;
        Long cnt = em.createQuery(
                        "SELECT COUNT(o) FROM Organization o WHERE o.owner.userId = :ownerId", Long.class)
                .setParameter("ownerId", owner.getUserId())
                .getSingleResult();
        return cnt != null && cnt > 0;
    }

    @Override
    public Organization save(Organization o) {
        if (o == null) return null;

        // Đặt mặc định tương đương SQL: reg_status mặc định PENDING, created_at SYSDATETIME()
        if (o.getRegStatus() == null) {
            o.setRegStatus(Organization.RegStatus.PENDING);
        }
        if (o.getCreatedAt() == null) {
            o.setCreatedAt(LocalDateTime.now());
        }

        // Insert nếu chưa có id, ngược lại merge (update)
        if (o.getOrgId() == null || o.getOrgId() == 0) {
            em.persist(o);         // o sẽ vào managed state, orgId được gán sau flush (với IDENTITY)
            // Nếu bạn cần đọc lại giá trị DB default ngay (ví dụ trigger), có thể:
            // em.flush(); em.refresh(o);
            return o;
        } else {
            return em.merge(o);    // trả về entity managed đã merge
        }
    }
}
