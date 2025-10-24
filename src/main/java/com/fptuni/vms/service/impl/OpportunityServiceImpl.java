package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.service.OpportunityService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityRepository opportunityRepository;

    public OpportunityServiceImpl(OpportunityRepository opportunityRepository) {
        this.opportunityRepository = opportunityRepository;
    }

    @Override
    public Page<OpportunityCardDto> getOpportunityCards(Pageable pageable) {
        Page<Opportunity> opportunities = opportunityRepository.findOpenOpportunities(pageable);
        List<OpportunityCardDto> dtos = opportunities.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, opportunities.getTotalElements());
    }

    @Override
    public Page<OpportunityCardDto> getOpportunityCardsWithFilters(
            Integer categoryId,
            String location,
            String status,
            String searchTerm,
            String time,
            String sortBy,
            Pageable pageable) {

        // Normalize parameters
        if (location != null && location.trim().isEmpty()) {
            location = null;
        }
        if (searchTerm != null && searchTerm.trim().isEmpty()) {
            searchTerm = null;
        }
        if (time != null && time.trim().isEmpty()) {
            time = null;
        }
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "newest";
        }

        // Convert status string to enum
        Opportunity.OpportunityStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = Opportunity.OpportunityStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        Page<Opportunity> opportunities = opportunityRepository.findOpportunitiesWithFilters(
                categoryId, location, statusEnum, searchTerm, time, sortBy, pageable);

        List<OpportunityCardDto> dtos = opportunities.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, opportunities.getTotalElements());
    }

    @Override
    public List<Category> getCategoriesWithOpportunities() {
        return opportunityRepository.findCategoriesWithOpportunities();
    }

    @Override
    public Opportunity findById(Integer id) {
        return opportunityRepository.findById(id).orElse(null);
    }

    @Override
    public List<OpportunityCardDto> getTop3LatestOpportunities() {
        Pageable pageable = PageRequest.of(0, 3);
        List<Opportunity> opportunities = opportunityRepository.findTop3LatestOpportunities(pageable);
        return opportunities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private OpportunityCardDto convertToDto(Opportunity opportunity) {
        OpportunityCardDto dto = new OpportunityCardDto();

        dto.setOppId(opportunity.getOppId());
        dto.setTitle(opportunity.getTitle());
        dto.setSubtitle(opportunity.getSubtitle());
        dto.setLocation(opportunity.getLocation());
        dto.setThumbnailUrl(opportunity.getThumbnailUrl());
        dto.setStatus(opportunity.getStatus());
        dto.setStartTime(opportunity.getStartTime());
        dto.setEndTime(opportunity.getEndTime());
        dto.setNeededVolunteers(opportunity.getNeededVolunteers());
        dto.setCreatedAt(opportunity.getCreatedAt());

        if (opportunity.getOrganization() != null) {
            dto.setOrganizationName(opportunity.getOrganization().getName());
            dto.setOrganizationVerified(
                    opportunity.getOrganization().getRegStatus() == Organization.RegStatus.APPROVED);
        }

        if (opportunity.getCategory() != null) {
            dto.setCategoryName(opportunity.getCategory().getCategoryName());
        }

        Long appliedCount = opportunityRepository.countApprovedApplications(opportunity.getOppId());
        dto.setAppliedVolunteers(appliedCount != null ? appliedCount.intValue() : 0);

        return dto;
    }

    @Override
    public List<Opportunity> getAll() {
        return opportunityRepository.getAll();
    }

    @Override
    public List<Opportunity> findByOrganization(int orgId) {
        return opportunityRepository.findByOrganization(orgId);
    }

    // @Override
    // public Opportunity findById(int id) {
    // return opportunityRepository.findById(id);
    // }
}
