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

import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public Opportunity save(Opportunity o) {
        if (o.getStatus() == null) {
            o.setStatus(Opportunity.OpportunityStatus.OPEN); // Đặt mặc định nếu chưa có
        }
        return opportunityRepository.save(o);
    }

    @Override
    public Page<Opportunity> searchByOrg(int orgId, String q,
            Opportunity.OpportunityStatus status, int page, int size, String timeOrder) {
        Pageable pageable = PageRequest.of(page, size);
        return opportunityRepository.searchByOrg(orgId, q, status, timeOrder, pageable);
    }

    // @Override
    // public Opportunity findById(int id) {
    // return opportunityRepository.findById(id);
    // }

    // === Volunteer view Org opportunities – trả entity trực tiếp === PhiLong iter
    // 3
    @Override
    public Page<Opportunity> getOrgOpportunities(
            int orgId,
            Integer categoryId,
            String keyword,
            String status,
            String quick,
            String sortBy,
            Pageable pageable) {
        Opportunity.OpportunityStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = Opportunity.OpportunityStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (sortBy == null || sortBy.isBlank())
            sortBy = "newest";

        return opportunityRepository.findOrgOpportunitiesWithFilters(
                orgId, categoryId, keyword, st, quick, sortBy, pageable);
    }

    // === NEW: helper count ===
    @Override
    public long countApproved(int oppId) {
        Long c = opportunityRepository.countApprovedApplications(oppId);
        return c == null ? 0L : c;
    }

    // ================= PHI LONG ITER 3 =================//
    @Override
    public List<Opportunity> findOverlapsForOrg(int orgId, Integer excludeOppId,
            LocalDateTime start, LocalDateTime end, int limit) {
        return opportunityRepository.findOverlapsForOrg(orgId, excludeOppId, start, end, limit);
    }

    @Override
    public Page<Opportunity> searchByOrgWithTimeState(Integer orgId,
                                                      String keyword,
                                                      String statusFilter,
                                                      int page,
                                                      int size,
                                                      String timeOrder) {
        return opportunityRepository.searchByOrgWithTimeState(orgId, keyword, statusFilter, page, size, timeOrder);
    }

}
