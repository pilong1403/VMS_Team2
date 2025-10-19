package com.fptuni.vms.repository;

import com.fptuni.vms.model.VolunteerRating;
import java.util.List;

public interface VolunteerRatingRepository {

    List<VolunteerRating> findByOrganization(int orgId, String keyword, Short stars, int offset, int limit);
    long countByOrganization(int orgId, String keyword, Short stars);
    long countPending(int orgId);
    long countDone(int orgId);
    VolunteerRating findById(int id);
    void save(VolunteerRating rating);
    void update(VolunteerRating rating);
}
