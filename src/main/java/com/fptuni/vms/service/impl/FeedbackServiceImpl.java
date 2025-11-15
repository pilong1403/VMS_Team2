package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.model.Feedback;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.FeedbackRepository;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<EventHistoryDto> getVolunteerEventHistory(int volunteerId, int page, int size) {
        int offset = page * size;
        return feedbackRepo.findVolunteerEventHistory(volunteerId, offset, size);
    }

    @Override
    public long countVolunteerEventHistory(int volunteerId) {
        return feedbackRepo.countVolunteerEventHistory(volunteerId);
    }

    @Override
    @Transactional
    public void createVolunteerFeedback(int oppId, int volunteerId, Integer rating, String content) {
        // Check if feedback already exists
        Feedback existingFeedback = feedbackRepo.findVolunteerFeedback(oppId, volunteerId);
        if (existingFeedback != null) {
            throw new IllegalStateException("Bạn đã đánh giá hoạt động này rồi!");
        }

        if (!canVolunteerGiveFeedback(oppId, volunteerId)) {
            throw new IllegalStateException("Bạn không thể đánh giá hoạt động này!");
        }

        Opportunity opportunity = opportunityRepository.findById(oppId);
        if (opportunity == null) {
            throw new IllegalArgumentException("Hoạt động không tồn tại!");
        }

        User volunteer = userRepository.findById(volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("Tình nguyện viên không tồn tại!"));

        Feedback feedback = new Feedback();
        feedback.setFeedbackId(null); // Explicitly set to null to ensure auto-generation
        feedback.setOpportunity(opportunity);
        feedback.setUser(volunteer);
        feedback.setRating(rating);
        feedback.setContent(content);
        feedback.setFeedbackType(Feedback.FeedbackType.VOLUNTEER);

        feedbackRepo.save(feedback);
    }

    @Override
    @Transactional
    public void updateVolunteerFeedback(int feedbackId, Integer rating, String content) {
        Feedback feedback = feedbackRepo.findById(feedbackId);
        if (feedback == null) {
            throw new IllegalArgumentException("Đánh giá không tồn tại!");
        }

        if (feedback.getCreatedAt().isBefore(LocalDateTime.now().minusDays(3))) {
            throw new IllegalStateException("Không thể chỉnh sửa đánh giá sau 3 ngày!");
        }

        feedback.setRating(rating);
        feedback.setContent(content);
        feedback.setUpdatedAt(LocalDateTime.now());

        feedbackRepo.update(feedback);
    }

    @Override
    public boolean canVolunteerGiveFeedback(int oppId, int volunteerId) {
        return feedbackRepo.canVolunteerGiveFeedback(oppId, volunteerId);
    }

    @Override
    public List<Feedback> findByOpportunity(int oppId) {
        return feedbackRepo.findByOpportunity(oppId);
    }

    @Override
    public Feedback findByOpportunityAndVolunteer(int oppId, int volunteerId) {
        return feedbackRepo.findVolunteerFeedback(oppId, volunteerId);
    }

    @Override
    public Feedback findById(int id) {
        return feedbackRepo.findById(id);
    }

    @Override
    public void save(Feedback feedback) {
        feedbackRepo.save(feedback);
    }

    @Override
    public void update(Feedback feedback) {
        feedbackRepo.update(feedback);
    }
}
