package com.fptuni.vms.repository.impl;

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

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {

    @PersistenceContext
    private EntityManager em; // container-managed, transaction-scoped

    // ===== 1) OPEN opportunities (fetch joins + phân trang)
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

    // ===== 2) Đếm application APPROVED/COMPLETED
    @Override
    public Long countApprovedApplications(Integer oppId) {
        if (oppId == null) return 0L;

        // Chỉnh enum theo entity Application của bạn nếu khác
        return em.createQuery(
                        "SELECT COUNT(a) FROM Application a " +
                                "WHERE a.opportunity.oppId = :id " +
                                "AND a.status IN (:s1, :s2)", Long.class)
                .setParameter("id", oppId)
                .setParameter("s1", com.fptuni.vms.model.Application.ApplicationStatus.APPROVED)
                .setParameter("s2", com.fptuni.vms.model.Application.ApplicationStatus.COMPLETED)
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

    // ===== 6) List theo org có filter text/category/status với offset/limit
    @Override
    public List<Opportunity> findByOrgIdPaged(int orgId, int offset, int limit, String q, Integer categoryId, String status) {
        StringBuilder where = new StringBuilder(" WHERE o.organization.orgId = :orgId ");
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", orgId);

        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :kw, '%')) " +
                    "  OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :kw, '%')) " +
                    "  OR LOWER(o.location) LIKE LOWER(CONCAT('%', :kw, '%'))) ");
            params.put("kw", q.trim());
        }
        if (categoryId != null) {
            where.append(" AND o.category.categoryId = :catId ");
            params.put("catId", categoryId);
        }
        if (status != null && !status.isBlank()) {
            Opportunity.OpportunityStatus st = Opportunity.OpportunityStatus.valueOf(status.trim().toUpperCase());
            where.append(" AND o.status = :st ");
            params.put("st", st);
        }

        String jpql =
                "SELECT o FROM Opportunity o " +
                        where +
                        " ORDER BY o.createdAt DESC ";

        TypedQuery<Opportunity> query = em.createQuery(jpql, Opportunity.class);
        params.forEach(query::setParameter);
        query.setFirstResult(Math.max(0, offset));
        query.setMaxResults(Math.max(1, limit));
        return query.getResultList();
    }

    // ===== 7) Đếm theo org + filter
    @Override
    public int countByOrgId(int orgId, String q, Integer categoryId, String status) {
        StringBuilder where = new StringBuilder(" WHERE o.organization.orgId = :orgId ");
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", orgId);

        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :kw, '%')) " +
                    "  OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :kw, '%')) " +
                    "  OR LOWER(o.location) LIKE LOWER(CONCAT('%', :kw, '%'))) ");
            params.put("kw", q.trim());
        }
        if (categoryId != null) {
            where.append(" AND o.category.categoryId = :catId ");
            params.put("catId", categoryId);
        }
        if (status != null && !status.isBlank()) {
            Opportunity.OpportunityStatus st = Opportunity.OpportunityStatus.valueOf(status.trim().toUpperCase());
            where.append(" AND o.status = :st ");
            params.put("st", st);
        }

        String countJpql = "SELECT COUNT(o) FROM Opportunity o " + where;

        TypedQuery<Long> query = em.createQuery(countJpql, Long.class);
        params.forEach(query::setParameter);
        Long total = query.getSingleResult();
        return total == null ? 0 : total.intValue();
    }

    // ===== 8) Tìm theo id + org
    @Override
    public Optional<Opportunity> findByIdAndOrg(int oppId, int orgId) {
        List<Opportunity> list = em.createQuery(
                        "SELECT o FROM Opportunity o WHERE o.oppId = :oppId AND o.organization.orgId = :orgId",
                        Opportunity.class)
                .setParameter("oppId", oppId)
                .setParameter("orgId", orgId)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
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

    // ===== 10) Xoá theo id + org
    @Override
    public boolean deleteByIdAndOrg(int oppId, int orgId) {
        Optional<Opportunity> opt = findByIdAndOrg(oppId, orgId);
        if (opt.isEmpty()) return false;
        em.remove(opt.get());
        return true;
    }

    // ===== 11) Recent by org + khoảng thời gian
    @Override
    public List<Opportunity> findRecentByOrg(int orgId, LocalDateTime from, LocalDateTime to) {
        return em.createQuery(
                        "SELECT o FROM Opportunity o " +
                                "WHERE o.organization.orgId = :orgId " +
                                "AND o.createdAt BETWEEN :from AND :to " +
                                "ORDER BY o.createdAt DESC",
                        Opportunity.class)
                .setParameter("orgId", orgId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
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
}
