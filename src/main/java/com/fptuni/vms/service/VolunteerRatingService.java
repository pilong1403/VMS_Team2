package com.fptuni.vms.service;

import com.fptuni.vms.model.VolunteerRating;

import java.util.List;

public interface VolunteerRatingService {

    // ===== CRUD =====
    void saveRating(VolunteerRating rating);
    VolunteerRating getRatingById(Integer id);
    void deleteRating(Integer id);
    List<VolunteerRating> getAllRatings();

    // ===== STATISTICS =====
    long countAllRatings();
    long countRatingsByStars(short stars);

    // ===== FILTER =====
    List<VolunteerRating> getRatingsByStars(short stars);
}
