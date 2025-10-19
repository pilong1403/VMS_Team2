package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import com.fptuni.vms.service.VolunteerRatingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VolunteerRatingServiceImpl implements VolunteerRatingService {

    private final VolunteerRatingRepository repo;

    @Override
    public List<VolunteerRating> findByOrganization(int orgId, String keyword, Short stars, int page, int size) {
        int offset = page * size;
        return repo.findByOrganization(orgId, keyword, stars, offset, size);
    }

    @Override
    public long countByOrganization(int orgId, String keyword, Short stars) {
        return repo.countByOrganization(orgId, keyword, stars);
    }

    @Override
    public long countPending(int orgId) {
        return repo.countPending(orgId);
    }

    @Override
    public long countDone(int orgId) {
        return repo.countDone(orgId);
    }

    @Override
    public VolunteerRating findById(int id) {
        return repo.findById(id);
    }

    @Override
    public void save(VolunteerRating rating) {
        repo.save(rating);
    }

    @Override
    public void update(VolunteerRating rating) {
        repo.update(rating);
    }
}
