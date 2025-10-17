package com.fptuni.vms.service;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OpportunityService {

    /**
     * Lấy danh sách cơ hội với phân trang
     */
    Page<OpportunityCardDto> getOpportunityCards(Pageable pageable);

    /**
     * Lấy danh sách cơ hội với bộ lọc
     */
    Page<OpportunityCardDto> getOpportunityCardsWithFilters(
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
