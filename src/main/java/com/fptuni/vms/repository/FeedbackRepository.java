package com.fptuni.vms.repository;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.model.Feedback;

import java.util.List;

public interface FeedbackRepository {

    List<EventHistoryDto> findVolunteerEventHistory(int volunteerId, int offset, int limit);

    long countVolunteerEventHistory(int volunteerId);

    boolean canVolunteerGiveFeedback(int oppId, int volunteerId);

    Feedback findVolunteerFeedback(int oppId, int volunteerId);

    List<Feedback> findByOpportunity(int oppId);

    Feedback findById(int id);

    void save(Feedback feedback);

    void update(Feedback feedback);

    long countAll();
}
