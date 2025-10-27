package com.fptuni.vms.service;

import com.fptuni.vms.dto.view.OpportunityCardDto;
import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OpportunityService {

    Page<OpportunityCardDto> getOpportunityCardsWithFilters(
            Integer categoryId,
            String location,
            String status,
            String searchTerm,
            String time,
            String sortBy,
            Pageable pageable);

    List<Category> getCategoriesWithOpportunities();

    Opportunity findById(Integer id);

    List<OpportunityCardDto> getTop3LatestOpportunities();

    Page<OpportunityCardDto> getOpportunityCards(Pageable pageable);

    List<Opportunity> getAll();

    List<Opportunity> findByOrganization(int orgId);

    Opportunity save(Opportunity o);

    Page<Opportunity> searchByOrg(int orgId, String q,
            Opportunity.OpportunityStatus status, int page, int size);
    // Opportunity findById(int id);

    // ================= PHI LONG ITER 3 =================//
    // Trang tổ chức - trả về Entity trực tiếp
    Page<Opportunity> getOrgOpportunities(
            int orgId,
            Integer categoryId,
            String keyword,
            String status, // OPEN|CLOSED|CANCELLED
            String quick, // upcoming|ongoing|past|null
            String sortBy,
            Pageable pageable);

    // đếm số đơn APPROVED/COMPLETED cho 1 opp (dùng để render progress)
    long countApproved(int oppId);
    // ================= PHI LONG ITER 3 =================//

}
