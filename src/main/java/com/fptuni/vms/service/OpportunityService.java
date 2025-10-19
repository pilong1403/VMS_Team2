package com.fptuni.vms.service;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OpportunityService {

    Page<OpportunityCardDto> getOpportunityCardsWithFilters(
            Integer categoryId,
            String location,
            String status,
            String searchTerm,
            String sortBy,
            Pageable pageable);

    List<Category> getCategoriesWithOpportunities();

    Opportunity findById(Integer id);

    List<OpportunityCardDto> getTop3LatestOpportunities();

    Page<OpportunityCardDto> getOpportunityCards(Pageable pageable);

    List<Opportunity> getAll();

    List<Opportunity> findByOrganization(int orgId);
    // Opportunity findById(int id);
}
