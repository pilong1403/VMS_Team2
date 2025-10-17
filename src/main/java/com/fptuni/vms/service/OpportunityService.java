package com.fptuni.vms.service;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OpportunityService {
    record Page<T>(List<T> items, int total, int page, int size) {}

    Page<Opportunity> listMyOpps(int ownerUserId, int page, int size, String q, Integer categoryId, String status);
    Optional<Opportunity> getMyOppById(int ownerUserId, int oppId);
    Opportunity createMyOpp(int ownerUserId, Opportunity opp);
    Opportunity updateMyOpp(int ownerUserId, Opportunity opp);
    boolean deleteMyOpp(int ownerUserId, int oppId);

    org.springframework.data.domain.Page<OpportunityCardDto> getOpportunityCards(Pageable pageable);

    /**
     * Lấy danh sách cơ hội với bộ lọc
     */
    org.springframework.data.domain.Page<OpportunityCardDto> getOpportunityCardsWithFilters(
            Integer categoryId,
            String location,
            String status,
            String searchTerm,
            String sortBy,
            Pageable pageable);

    /**
     * Lấy danh sách categories có cơ hội
     */
    List<Category> getCategoriesWithOpportunities();

    /**
     * Tìm cơ hội theo ID
     */
    Opportunity findById(Integer id);

    /**
     * Lấy top 3 cơ hội mới nhất cho trang home
     */
    List<OpportunityCardDto> getTop3LatestOpportunities();
}
