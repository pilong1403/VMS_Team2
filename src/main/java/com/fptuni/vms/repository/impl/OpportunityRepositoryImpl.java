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
        String dataJpql = "SELECT o FROM Opportunity o " +
                "JOIN FETCH o.organization " +
                "JOIN FETCH o.category " +
                "WHERE o.status = :st " +
                "ORDER BY o.createdAt DESC";

        String countJpql = "SELECT COUNT(o) FROM Opportunity o " +
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
        if (oppId == null)
            return 0L;

        return em.createQuery(
                "SELECT COUNT(a) FROM Application a " +
                        "WHERE a.opportunity.oppId = :id " +
                        "AND a.status IN (:s1, :s2)",
                Long.class)
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
            String time,
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

        // Time filter logic
        if (time != null && !time.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (time.toLowerCase()) {
                case "today":
                    LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                    LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
                    where.append(" AND ((o.startTime >= :startOfDay AND o.startTime <= :endOfDay) " +
                            "OR (o.endTime >= :startOfDay AND o.endTime <= :endOfDay) " +
                            "OR (o.startTime <= :startOfDay AND o.endTime >= :endOfDay)) ");
                    params.put("startOfDay", startOfDay);
                    params.put("endOfDay", endOfDay);
                    break;
                case "week":
                    LocalDateTime startOfWeek = now.toLocalDate().atStartOfDay()
                            .minusDays(now.getDayOfWeek().getValue() - 1);
                    LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
                    where.append(" AND ((o.startTime >= :startOfWeek AND o.startTime <= :endOfWeek) " +
                            "OR (o.endTime >= :startOfWeek AND o.endTime <= :endOfWeek) " +
                            "OR (o.startTime <= :startOfWeek AND o.endTime >= :endOfWeek)) ");
                    params.put("startOfWeek", startOfWeek);
                    params.put("endOfWeek", endOfWeek);
                    break;
                case "month":
                    LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                    LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusDays(1).withHour(23).withMinute(59)
                            .withSecond(59);
                    where.append(" AND ((o.startTime >= :startOfMonth AND o.startTime <= :endOfMonth) " +
                            "OR (o.endTime >= :startOfMonth AND o.endTime <= :endOfMonth) " +
                            "OR (o.startTime <= :startOfMonth AND o.endTime >= :endOfMonth)) ");
                    params.put("startOfMonth", startOfMonth);
                    params.put("endOfMonth", endOfMonth);
                    break;
            }
        }

        String orderClause = ("deadline".equalsIgnoreCase(sortBy))
                ? " ORDER BY o.endTime ASC "
                : " ORDER BY o.createdAt DESC ";

        // Data JPQL (có fetch join)
        String dataJpql = "SELECT o FROM Opportunity o " +
                "JOIN FETCH o.organization org " +
                "JOIN FETCH o.category c " +
                where + orderClause;

        // Count JPQL (không fetch join)
        String countJpql = "SELECT COUNT(o) FROM Opportunity o " +
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
                        "WHERE o.status = :st",
                Category.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .getResultList();
    }

    // ===== 5) Top 3 latest OPEN (fetch joins)
    @Override
    public List<Opportunity> findTop3LatestOpportunities(Pageable pageable) {
        int size = pageable != null ? pageable.getPageSize() : 3;
        if (size <= 0 || size > 3)
            size = 3;

        return em.createQuery(
                "SELECT o FROM Opportunity o " +
                        "JOIN FETCH o.organization " +
                        "JOIN FETCH o.category " +
                        "WHERE o.status = :st " +
                        "ORDER BY o.createdAt DESC",
                Opportunity.class)
                .setParameter("st", Opportunity.OpportunityStatus.OPEN)
                .setMaxResults(size)
                .getResultList();
    }

    // ===== 9) Lưu (insert/update)
    @Override
    public Opportunity save(Opportunity o) {
        if (o == null)
            return null;
        if (o.getOppId() == null || o.getOppId() == 0) {
            em.persist(o);
            return o;
        } else {
            return em.merge(o);
        }
    }

    @Override
    public Optional<Opportunity> findById(Integer id) {
        if (id == null)
            return Optional.empty();
        List<Opportunity> list = em.createQuery(
                "SELECT o FROM Opportunity o " +
                        "LEFT JOIN FETCH o.organization " +
                        "LEFT JOIN FETCH o.category " +
                        "WHERE o.oppId = :id",
                Opportunity.class)
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

    @Override
    public Page<Opportunity> searchByOrg(int orgId, String q,
            Opportunity.OpportunityStatus status, String timeOrder, Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE org.orgId = :orgId ");
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", orgId);

        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  OR LOWER(o.location) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :q, '%'))) ");
            params.put("q", q.trim());
        }
        if (status != null) {
            where.append(" AND o.status = :st ");
            params.put("st", status);
        }

        // --- sort giống controls Approvals ---
        String order;
        if ("asc".equalsIgnoreCase(timeOrder)) {
            order = " ORDER BY o.createdAt ASC ";
        } else if ("deadline".equalsIgnoreCase(timeOrder)) {
            order = " ORDER BY o.endTime ASC ";
        } else { // default "desc" (mới nhất)
            order = " ORDER BY o.createdAt DESC ";
        }

        String dataJpql = "SELECT o FROM Opportunity o " +
                "JOIN o.organization org " +
                "LEFT JOIN FETCH o.category c " +
                where + order;

        String countJpql = "SELECT COUNT(o) FROM Opportunity o " +
                "JOIN o.organization org " +
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

    // === Volunteer View : Org scope + keyword + quick chips === PhiLong iter 3
    @Override
    public Page<Opportunity> findOrgOpportunitiesWithFilters(
            int orgId,
            Integer categoryId,
            String keyword,
            Opportunity.OpportunityStatus status,
            String quick,
            String sortBy,
            Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE org.orgId = :orgId ");
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", orgId);

        if (categoryId != null) {
            where.append(" AND c.categoryId = :catId ");
            params.put("catId", categoryId);
        }
        if (status != null) {
            // Nếu có trạng thái cụ thể => lọc chính xác
            where.append(" AND o.status = :st ");
            params.put("st", status);
        } else {
            // Nếu chọn "Tất cả" => chỉ lấy các trạng thái hợp lệ
            where.append(" AND o.status IN (:st1, :st2, :st3) ");
            params.put("st1", Opportunity.OpportunityStatus.OPEN);
            params.put("st2", Opportunity.OpportunityStatus.CLOSED);
            params.put("st3", Opportunity.OpportunityStatus.CANCELLED);
        }

        if (keyword != null && !keyword.isBlank()) {
            where.append("""
                        AND (
                             LOWER(o.title)    LIKE LOWER(CONCAT('%', :kw, '%'))
                          OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :kw, '%'))
                          OR LOWER(o.location) LIKE LOWER(CONCAT('%', :kw, '%'))
                        )
                    """);
            params.put("kw", keyword.trim());
        }

        if (quick != null && !quick.isBlank()) {
            switch (quick.toLowerCase()) {
                case "upcoming" -> where.append(" AND o.startTime >= CURRENT_TIMESTAMP ");
                case "ongoing" ->
                    where.append(" AND o.startTime <= CURRENT_TIMESTAMP AND o.endTime >= CURRENT_TIMESTAMP ");
                case "past" -> where.append(" AND o.endTime < CURRENT_TIMESTAMP ");
                default -> {
                    /* no-op */ }
            }
        }

        String orderClause = ("deadline".equalsIgnoreCase(sortBy))
                ? " ORDER BY o.endTime ASC "
                : " ORDER BY o.createdAt DESC ";

        String dataJpql = "SELECT o FROM Opportunity o " +
                "JOIN FETCH o.organization org " +
                "JOIN FETCH o.category c " +
                where + orderClause;

        String countJpql = "SELECT COUNT(o) FROM Opportunity o " +
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

    @Override
    public List<Opportunity> findOverlapsForOrg(int orgId, Integer excludeOppId,
            LocalDateTime start, LocalDateTime end, int limit) {
        if (start == null || end == null)
            return List.of();

        String jpql = """
                SELECT o
                FROM Opportunity o
                JOIN o.organization org
                WHERE org.orgId = :orgId
                  AND o.status IN (:st1, :st2)
                  AND (:excludeId IS NULL OR o.oppId <> :excludeId)
                  AND (:pStart < o.endTime AND :pEnd > o.startTime)
                ORDER BY o.startTime ASC
                """;

        var q = em.createQuery(jpql, Opportunity.class)
                .setParameter("orgId", orgId)
                .setParameter("st1", Opportunity.OpportunityStatus.DRAFT)
                .setParameter("st2", Opportunity.OpportunityStatus.OPEN)
                .setParameter("excludeId", excludeOppId)
                .setParameter("pStart", start)
                .setParameter("pEnd", end);

        if (limit > 0)
            q.setMaxResults(limit);
        return q.getResultList();
    }

    @Override
    public long countAll() {
        String jpql = "SELECT COUNT(o) FROM Opportunity o";
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

}
