package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OpportunityRepository {

    Page<Opportunity> findOpenOpportunities(Pageable pageable);

    Long countApprovedApplications(Integer oppId);

    Page<Opportunity> findOpportunitiesWithFilters(
            Integer categoryId,
            String location,
            Opportunity.OpportunityStatus status,
            String searchTerm,
            String sortBy,
            Pageable pageable);

    List<com.fptuni.vms.model.Category> findCategoriesWithOpportunities();

    List<Opportunity> findTop3LatestOpportunities(Pageable pageable);

    List<Opportunity> findByOrgIdPaged(int orgId, int offset, int limit, String q, Integer categoryId, String status);

    int countByOrgId(int orgId, String q, Integer categoryId, String status);

    Optional<Opportunity> findByIdAndOrg(int oppId, int orgId);

    // THÊM HÀM ĐANG ĐƯỢC SERVICE GỌI
    Optional<Opportunity> findById(Integer id);

    Opportunity save(Opportunity o);

    boolean deleteByIdAndOrg(int oppId, int orgId);

    List<Opportunity> findRecentByOrg(int orgId, LocalDateTime from, LocalDateTime to);
    List<Opportunity> getAll();
    List<Opportunity> findByOrganization(int orgId);
    Opportunity findById(int id);

}
