package com.fptuni.vms.service;

import com.fptuni.vms.model.VolunteerRating;

import java.util.List;

public interface VolunteerRatingService {

    List<VolunteerRating> findByOrganization(int orgId, String keyword, Short stars, int page, int size);
    long countByOrganization(int orgId, String keyword, Short stars);
    long countPending(int orgId);
    long countDone(int orgId);
    VolunteerRating findById(int id);
    void save(VolunteerRating rating);
    void update(VolunteerRating rating);
}
