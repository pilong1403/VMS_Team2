package com.fptuni.vms.service;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.model.Feedback;

import java.util.List;

public interface FeedbackService {

    List<EventHistoryDto> getVolunteerEventHistory(int volunteerId, int page, int size);

    long countVolunteerEventHistory(int volunteerId);

    void createVolunteerFeedback(int oppId, int volunteerId, Integer rating, String content);

    void updateVolunteerFeedback(int feedbackId, Integer rating, String content);

    boolean canVolunteerGiveFeedback(int oppId, int volunteerId);

    List<Feedback> findByOpportunity(int oppId);

    Feedback findByOpportunityAndVolunteer(int oppId, int volunteerId);

    Feedback findById(int id);

    void save(Feedback feedback);

    void update(Feedback feedback);
}
