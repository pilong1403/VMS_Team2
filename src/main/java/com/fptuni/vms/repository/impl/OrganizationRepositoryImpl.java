package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.OrganizationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void save(Organization organization) {
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

    @Override
    public Organization findByOwnerId(Integer ownerId) {
        List<Organization> list = em.createQuery("""
                SELECT o FROM Organization o 
                JOIN FETCH o.owner ow 
                WHERE ow.userId = :ownerId
                """, Organization.class)
                .setParameter("ownerId", ownerId)
                .setMaxResults(1)
                .getResultList();

        return list.isEmpty() ? null : list.get(0);
    }
}
