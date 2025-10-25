package com.fptuni.vms.service;

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
