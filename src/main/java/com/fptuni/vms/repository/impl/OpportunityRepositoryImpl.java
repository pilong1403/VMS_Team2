package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.repository.OpportunityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import jakarta.persistence.NoResultException;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Opportunity> findOpenOpportunities(Pageable pageable) {
        String dataJpql =
                "SELECT o FROM Opportunity o " +
                        "JOIN FETCH o.organization " +
                        "JOIN FETCH o.category " +
                        "WHERE o.status = :st " +
                        "ORDER BY o.createdAt DESC";

        String countJpql =
                "SELECT COUNT(o) FROM Opportunity o " +
                        "WHERE o.status = :st";

        TypedQuery<Opportunity> dataQ = em.createQuery(dataJpql, Opportunity.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        List<Opportunity> content = dataQ.getResultList();

        Long total = em.createQuery(countJpql, Long.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Long countApprovedApplications(Integer oppId) {
        if (oppId == null) return 0L;

        return em.createQuery(
                        "SELECT COUNT(a) FROM Application a " +
                                "WHERE a.opportunity.oppId = :id " +
                                "AND a.status IN (:s1, :s2)", Long.class)
                .setParameter("id", oppId)
                .setParameter("s1", Application.ApplicationStatus.APPROVED)
                .setParameter("s2", Application.ApplicationStatus.COMPLETED)
                .getSingleResult();
    }

    // ===== 3) Tìm có filter + sort + phân trang (chuỗi JPQL hoàn chỉnh)
    @Override
    public Page<Opportunity> findOpportunitiesWithFilters(
            Integer categoryId,
            String location,
            Opportunity.OpportunityStatus status,
            String searchTerm,
            String sortBy,
            Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (categoryId != null) {
            where.append(" AND c.categoryId = :catId ");
            params.put("catId", categoryId);
        }
        if (location != null && !location.isBlank()) {
            where.append(" AND LOWER(o.location) LIKE LOWER(CONCAT('%', :loc, '%')) ");
            params.put("loc", location.trim());
        }
        if (status != null) {
            where.append(" AND o.status = :st ");
            params.put("st", status);
        }
        if (searchTerm != null && !searchTerm.isBlank()) {
            where.append(" AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :q, '%'))) ");
            params.put("q", searchTerm.trim());
        }

        String orderClause = ("deadline".equalsIgnoreCase(sortBy))
                ? " ORDER BY o.endTime ASC "
                : " ORDER BY o.createdAt DESC ";

        // Data JPQL (có fetch join)
        String dataJpql =
                "SELECT o FROM Opportunity o " +
                        "JOIN FETCH o.organization org " +
                        "JOIN FETCH o.category c " +
                        where + orderClause;

        // Count JPQL (không fetch join)
        String countJpql =
                "SELECT COUNT(o) FROM Opportunity o " +
                        "JOIN o.organization org " +
                        "JOIN o.category c " +
                        where;

        TypedQuery<Opportunity> dataQ = em.createQuery(dataJpql, Opportunity.class);
        params.forEach(dataQ::setParameter);
        dataQ.setFirstResult((int) pageable.getOffset());
        dataQ.setMaxResults(pageable.getPageSize());
        List<Opportunity> content = dataQ.getResultList();

        TypedQuery<Long> cntQ = em.createQuery(countJpql, Long.class);
        params.forEach(cntQ::setParameter);
        Long total = cntQ.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    // ===== 4) Category có cơ hội OPEN
    @Override
    public List<Category> findCategoriesWithOpportunities() {
        return em.createQuery(
                        "SELECT DISTINCT c FROM Category c " +
                                "JOIN Opportunity o ON o.category = c " +
                                "WHERE o.status = :st", Category.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .getResultList();
    }

    // ===== 5) Top 3 latest OPEN (fetch joins)
    @Override
    public List<Opportunity> findTop3LatestOpportunities(Pageable pageable) {
        int size = pageable != null ? pageable.getPageSize() : 3;
        if (size <= 0 || size > 3) size = 3;

        return em.createQuery(
                        "SELECT o FROM Opportunity o " +
                                "JOIN FETCH o.organization " +
                                "JOIN FETCH o.category " +
                                "WHERE o.status = :st " +
                                "ORDER BY o.createdAt DESC", Opportunity.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .setMaxResults(size)
                .getResultList();
    }

    // ===== 9) Lưu (insert/update)
    @Override
    public Opportunity save(Opportunity o) {
        if (o == null) return null;
        if (o.getOppId() == null || o.getOppId() == 0) {
            em.persist(o);
            return o;
        } else {
            return em.merge(o);
        }
    }

    @Override
    public Optional<Opportunity> findById(Integer id) {
        if (id == null) return Optional.empty();
        List<Opportunity> list = em.createQuery(
                        "SELECT o FROM Opportunity o " +
                                "LEFT JOIN FETCH o.organization " +
                                "LEFT JOIN FETCH o.category " +
                                "WHERE o.oppId = :id", Opportunity.class)
                .setParameter("id", id)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }


    @Override
    public List<Opportunity> getAll() {
        return em.createQuery("""
                SELECT o 
                FROM Opportunity o 
                JOIN FETCH o.organization org 
                LEFT JOIN FETCH o.category c
                ORDER BY o.createdAt DESC
                """, Opportunity.class)
                .getResultList();
    }


    @Override
    public List<Opportunity> findByOrganization(int orgId) {
        return em.createQuery("""
                SELECT o 
                FROM Opportunity o 
                JOIN FETCH o.organization org 
                LEFT JOIN FETCH o.category c
                WHERE org.orgId = :orgId
                ORDER BY o.createdAt DESC
                """, Opportunity.class)
                .setParameter("orgId", orgId)
                .getResultList();
    }

    @Override
    public Opportunity findById(int id) {
        try {
            return em.createQuery("""
                    SELECT o
                    FROM Opportunity o
                    JOIN FETCH o.organization org
                    LEFT JOIN FETCH o.category c
                    WHERE o.oppId = :id
                    """, Opportunity.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // tránh exception nếu không tìm thấy
        }
    }
}
