package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityRepository {

    Page<Opportunity> findOpenOpportunities(Pageable pageable);

    Long countApprovedApplications(Integer oppId);

    Page<Opportunity> findOpportunitiesWithFilters(
            Integer categoryId,
            String location,
            Opportunity.OpportunityStatus status,
            String searchTerm,
            String time,
            String sortBy,
            Pageable pageable);

    List<com.fptuni.vms.model.Category> findCategoriesWithOpportunities();

    List<Opportunity> findTop3LatestOpportunities(Pageable pageable);

    // THÊM HÀM ĐANG ĐƯỢC SERVICE GỌI
    Optional<Opportunity> findById(Integer id);

    Opportunity save(Opportunity o);

    List<Opportunity> getAll();

    List<Opportunity> findByOrganization(int orgId);

    Opportunity findById(int id);

}
