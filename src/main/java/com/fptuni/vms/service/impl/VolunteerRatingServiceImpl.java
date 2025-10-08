package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import com.fptuni.vms.service.VolunteerRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VolunteerRatingServiceImpl implements VolunteerRatingService {

    @Autowired
    private VolunteerRatingRepository ratingRepository;

    // ===== CRUD =====
    @Override
    public void saveRating(VolunteerRating rating) {
        ratingRepository.save(rating);
    }

    @Override
    public VolunteerRating getRatingById(Integer id) {
        // Có thể trả về Optional, ở đây đơn giản dùng em.find
        // cần thêm hàm findById trong repo nếu muốn
        throw new UnsupportedOperationException("Chưa cài đặt findById trong repo");
    }

    @Override
    public void deleteRating(Integer id) {
        // Có thể cài đặt nếu cần, thêm hàm deleteById trong repo
        throw new UnsupportedOperationException("Chưa cài đặt deleteById trong repo");
    }

    @Override
    public List<VolunteerRating> getAllRatings() {
        // Có thể cài đặt nếu cần, thêm hàm findAll trong repo
        throw new UnsupportedOperationException("Chưa cài đặt findAll trong repo");
    }

    // ===== STATISTICS =====
    @Override
    public long countAllRatings() {
        return ratingRepository.countAll();
    }

    @Override
    public long countRatingsByStars(short stars) {
        return ratingRepository.countByStars(stars);
    }

    // ===== FILTER =====
    @Override
    public List<VolunteerRating> getRatingsByStars(short stars) {
        return ratingRepository.findByStars(stars);
    }
}
