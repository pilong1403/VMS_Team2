// src/main/java/com/fptuni/vms/repository/impl/OrganizationRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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



    @Override
    public void save1(Organization organization) {
        if (organization.getOrgId() == null) {
            em.persist(organization);
        } else {
            em.merge(organization);
        }
    }

    @Override
    public Organization findById(Integer id) {
        return em.find(Organization.class, id);
    }



    @Override
    public List<Organization> search(String keyword, Organization.RegStatus status,
                                     LocalDate fromDate, LocalDate toDate,
                                     int page, int size, String sortDir, String sortField) {

        // Kiểm tra sortField hợp lệ
        if (sortField == null || sortField.isBlank()) sortField = "createdAt";
        if (sortDir == null || sortDir.isBlank()) sortDir = "DESC";

        StringBuilder jpql = new StringBuilder("SELECT o FROM Organization o WHERE 1=1");

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(o.name) LIKE LOWER(:kw) OR LOWER(o.owner.fullName) LIKE LOWER(:kw))");
        }
        if (status != null) {
            jpql.append(" AND o.regStatus = :status");
        }
        if (fromDate != null) {
            jpql.append(" AND o.createdAt >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" AND o.createdAt <= :toDate");
        }

        // tránh injection
        jpql.append(" ORDER BY o.")
                .append(sortField)
                .append(" ")
                .append(sortDir.equalsIgnoreCase("ASC") ? "ASC" : "DESC");

        TypedQuery<Organization> query = em.createQuery(jpql.toString(), Organization.class);

        if (keyword != null && !keyword.isBlank()) query.setParameter("kw", "%" + keyword + "%");
        if (status != null) query.setParameter("status", status);
        if (fromDate != null) query.setParameter("fromDate", fromDate.atStartOfDay());
        if (toDate != null) query.setParameter("toDate", toDate.plusDays(1).atStartOfDay());

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public long countFiltered(String keyword, Organization.RegStatus status,
                              LocalDate fromDate, LocalDate toDate) {

        StringBuilder jpql = new StringBuilder("SELECT COUNT(o) FROM Organization o WHERE 1=1");

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(o.name) LIKE LOWER(:kw) OR LOWER(o.owner.fullName) LIKE LOWER(:kw))");
        }
        if (status != null) {
            jpql.append(" AND o.regStatus = :status");
        }
        if (fromDate != null) {
            jpql.append(" AND o.createdAt >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" AND o.createdAt <= :toDate");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (keyword != null && !keyword.isBlank()) query.setParameter("kw", "%" + keyword + "%");
        if (status != null) query.setParameter("status", status);
        if (fromDate != null) query.setParameter("fromDate", fromDate.atStartOfDay());
        if (toDate != null) query.setParameter("toDate", toDate.plusDays(1).atStartOfDay());

        return query.getSingleResult();
    }

    @Override
    public long countAll() {
        return em.createQuery("SELECT COUNT(o) FROM Organization o", Long.class).getSingleResult();
    }

    @Override
    public List<Organization> getOrganizationByAPPROVED() {
        String jpql = "SELECT o FROM Organization o WHERE o.regStatus = :status ORDER BY o.createdAt DESC";
        return em.createQuery(jpql, Organization.class)
                .setParameter("status", Organization.RegStatus.APPROVED)
                .getResultList();
    }

//    @Override
//    public Organization findByOwnerId(Integer ownerId) {
//        List<Organization> list = em.createQuery("""
//                SELECT o FROM Organization o
//                JOIN FETCH o.owner ow
//                WHERE ow.userId = :ownerId
//                """, Organization.class)
//                .setParameter("ownerId", ownerId)
//                .setMaxResults(1)
//                .getResultList();
//
//        return list.isEmpty() ? null : list.get(0);
//    }
}
