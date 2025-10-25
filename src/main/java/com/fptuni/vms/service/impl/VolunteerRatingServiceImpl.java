package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.response.OpportunitySummaryDto;
import com.fptuni.vms.dto.response.OpportunityVolunteerRatingDto;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import com.fptuni.vms.service.VolunteerRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VolunteerRatingServiceImpl implements VolunteerRatingService {

    @Autowired
    private VolunteerRatingRepository ratingRepo;
    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private UserRepository userRepository;

    // ===================== HOẠT ĐỘNG =====================
    @Override
    public List<OpportunitySummaryDto> findOpportunitiesByOrg(int orgId, String keyword,
                                                              String eventStatus, String sort,
                                                              int offset, int limit) {
        return ratingRepo.findOpportunitiesByOrg(orgId, keyword, eventStatus, sort, offset, limit);
    }

    @Override
    public long countOpportunitiesByOrg(int orgId, String keyword, String eventStatus) {
        return ratingRepo.countOpportunitiesByOrg(orgId, keyword, eventStatus);
    }

    // ===================== TÌNH NGUYỆN VIÊN TRONG HOẠT ĐỘNG =====================
    @Override
    public List<OpportunityVolunteerRatingDto> getVolunteersForOpportunity(int orgId, int opportunityId,
                                                                           String keyword, String statusFilter,
                                                                           String sort, int offset, int limit) {
        return ratingRepo.findVolunteersForOpportunity(orgId, opportunityId, keyword, statusFilter, sort, offset, limit);
    }

    @Override
    public long countVolunteersForOpportunity(int orgId, int opportunityId, String keyword) {
        // Mặc định đếm tất cả trạng thái
        return ratingRepo.countVolunteersForOpportunity(orgId, opportunityId, keyword, "ALL");
    }

    // ===================== BADGE =====================
    @Override
    public double getAverageStarsByUser(int userId) {
        Double avg = ratingRepo.getAverageStarsByUser(userId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public long countPendingAll(int orgId) {
        return ratingRepo.countPendingAll(orgId);
    }

    @Override
    public long countRatedAll(int orgId) {
        return ratingRepo.countRatedAll(orgId);
    }

    @Override
    public void createRating(int oppId, int rateeUserId, int orgId, VolunteerRating rating) {

        // 1. Kiểm tra sự kiện có tồn tại không
        Opportunity opportunity = opportunityRepository.findById(oppId);
        if (opportunity == null) {
            throw new IllegalArgumentException("Hoạt động không tồn tại!");
        }

        // 2. Kiểm tra user có tồn tại không
        User ratee = userRepository.findById(rateeUserId)
                .orElseThrow(() -> new IllegalArgumentException("Người được đánh giá không tồn tại!"));

        // 3. Kiểm tra đã CHECK-IN chưa (KHÔNG cần checkout nữa)
        if (!ratingRepo.hasCheckedIn(oppId, rateeUserId)) {
            throw new IllegalStateException("Tình nguyện viên chưa CHECK-IN, không thể đánh giá!");
        }

        // 4. Kiểm tra đã từng đánh giá chưa
        if (ratingRepo.hasRated(oppId, rateeUserId, orgId)) {
            throw new IllegalStateException("Tình nguyện viên này đã được đánh giá rồi!");
        }

        // 5. Thiết lập dữ liệu để lưu
        rating.setOpportunity(opportunity);
        rating.setRateeUser(ratee);
        rating.setCreatedAt(LocalDateTime.now());
        rating.setRaterOrg(opportunity.getOrganization()); // hoặc lấy từ session

        // 6. Lưu
        ratingRepo.save(rating);
    }
    // ===================== CRUD =====================
    @Override
    public VolunteerRating findById(int id) {
        return ratingRepo.findById(id);
    }

    @Override
    public void save(VolunteerRating rating) {
        ratingRepo.save(rating);
    }

    @Override
    public void update(VolunteerRating rating) {
        ratingRepo.update(rating);
    }
}
