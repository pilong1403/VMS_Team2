// src/main/java/com/fptuni/vms/repository/OpportunityRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Integer> {

    /**
     * Lấy danh sách cơ hội cơ bản
     */
    @Query("SELECT o FROM Opportunity o JOIN FETCH o.organization JOIN FETCH o.category WHERE o.status = 'OPEN' ORDER BY o.createdAt DESC")
    Page<Opportunity> findOpenOpportunities(Pageable pageable);

    /**
     * Đếm số lượng applications đã approved cho một opportunity
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.opportunity.oppId = :oppId AND a.status IN ('APPROVED', 'COMPLETED')")
    Long countApprovedApplications(@Param("oppId") Integer oppId);

    /**
     * Lấy danh sách cơ hội với bộ lọc
     */
    @Query("""
                SELECT o FROM Opportunity o
                JOIN FETCH o.organization org
                JOIN FETCH o.category c
                WHERE (:categoryId IS NULL OR c.categoryId = :categoryId)
                AND (:location IS NULL OR LOWER(o.location) LIKE LOWER(CONCAT('%', :location, '%')))
                AND (:status IS NULL OR o.status = :status)
                AND (:searchTerm IS NULL OR
                     LOWER(o.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                     LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                ORDER BY
                    CASE WHEN :sortBy = 'deadline' THEN o.endTime END ASC,
                    o.createdAt DESC
            """)
    Page<Opportunity> findOpportunitiesWithFilters(
            @Param("categoryId") Integer categoryId,
            @Param("location") String location,
            @Param("status") Opportunity.OpportunityStatus status,
            @Param("searchTerm") String searchTerm,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    /**
     * Lấy danh sách categories có cơ hội
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN Opportunity o ON o.category = c WHERE o.status = 'OPEN'")
    List<com.fptuni.vms.model.Category> findCategoriesWithOpportunities();

    /**
     * Lấy top 3 cơ hội mới nhất cho trang home
     */
    @Query("SELECT o FROM Opportunity o JOIN FETCH o.organization JOIN FETCH o.category WHERE o.status = 'OPEN' ORDER BY o.createdAt DESC")
    List<Opportunity> findTop3LatestOpportunities(Pageable pageable);

    List<Opportunity> findByOrgIdPaged(int orgId, int offset, int limit, String q, Integer categoryId, String status);
    int countByOrgId(int orgId, String q, Integer categoryId, String status);
    Optional<Opportunity> findByIdAndOrg(int oppId, int orgId);
    Opportunity save(Opportunity o);
    boolean deleteByIdAndOrg(int oppId, int orgId);

    // tiện lợi:
    List<Opportunity> findRecentByOrg(int orgId, LocalDateTime from, LocalDateTime to);
}
