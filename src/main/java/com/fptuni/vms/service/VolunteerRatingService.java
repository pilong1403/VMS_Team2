package com.fptuni.vms.service;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.dto.response.OpportunitySummaryDto;
import com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto;
import com.fptuni.vms.model.VolunteerRating;

import java.util.List;

public interface VolunteerRatingService {

    // --------- Hoạt động ---------
    List<OpportunitySummaryDto> findOpportunitiesByOrg(int orgId, String keyword,
            String eventStatus, String sort,
            int offset, int limit);

    long countOpportunitiesByOrg(int orgId, String keyword, String eventStatus);

    // --------- TNV trong hoạt động ---------
    List<OpportunityVolunteerRatingDto> getVolunteersForOpportunity(int orgId, int opportunityId,
            String keyword, String statusFilter,
            String sort, int offset, int limit);

    long countVolunteersForOpportunity(int orgId, int opportunityId, String keyword);

    // --------- Volunteer Event History ---------
    List<EventHistoryDto> getVolunteerEventHistory(int volunteerId, int page, int size);

    long countVolunteerEventHistory(int volunteerId);

    // --------- Volunteer Rating ---------
    void createVolunteerRating(int oppId, int volunteerId, Short stars, String comment);

    void updateVolunteerRating(int ratingId, Short stars, String comment);

    boolean canVolunteerRate(int oppId, int volunteerId);

    // --------- Badge ---------
    double getAverageStarsByUser(int userId);

    long countPendingAll(int orgId);

    long countRatedAll(int orgId);

    void createRating(int oppId, int rateeUserId, int orgId, VolunteerRating rating);

    // --------- CRUD ---------
    VolunteerRating findById(int id);

    void save(VolunteerRating rating);

    void update(VolunteerRating rating);
}
