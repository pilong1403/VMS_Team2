package com.fptuni.vms.repository;

import com.fptuni.vms.dto.response.OpportunitySummaryDto;
import com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto;
import com.fptuni.vms.model.VolunteerRating;

import java.util.List;

public interface VolunteerRatingRepository {

    // ================================
    // 1. DANH SÁCH HOẠT ĐỘNG (LIST PAGE)
    // ================================
    List<OpportunitySummaryDto> findOpportunitiesByOrg(
            int orgId,
            String keyword,
            String eventStatus,   // UPCOMING, ONGOING, FINISHED, ALL
            String sort,          // start, end, name
            int offset,
            int limit
    );

    long countOpportunitiesByOrg(
            int orgId,
            String keyword,
            String eventStatus
    );

    // ================================
    // 2. DANH SÁCH TÌNH NGUYỆN VIÊN TRONG HOẠT ĐỘNG
    // ================================
    List<OpportunityVolunteerRatingDto> findVolunteersForOpportunity(
            int orgId,
            int opportunityId,
            String keyword,
            String statusFilter,  // NOT_ATTENDED, PENDING, RATED, ALL
            String sort,          // name, hours, checkin
            int offset,
            int limit
    );

    long countVolunteersForOpportunity(
            int orgId,
            int opportunityId,
            String keyword,
            String statusFilter
    );

    // ================================
    // 3. CRUD VOLUNTEER RATING
    // ================================
    VolunteerRating findById(int id);

    void save(VolunteerRating rating);

    void update(VolunteerRating rating);

    // ================================
    // 4. BADGE (THỐNG KÊ)
    // ================================
    Double getAverageStarsByUser(int userId);
    boolean hasCheckedIn(int oppId, int userId);
    boolean hasRated(int oppId, int userId, int orgId);

    long countPendingAll(int orgId);  // Tổng người cần đánh giá
    long countRatedAll(int orgId);    // Tổng người đã đánh giá
}
