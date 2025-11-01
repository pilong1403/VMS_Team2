package com.fptuni.vms.repository;

import com.fptuni.vms.dto.EventHistoryDto;
import com.fptuni.vms.model.Feedback;

import java.util.List;

public interface FeedbackRepository {

    // ================================
    // 1. VOLUNTEER EVENT HISTORY
    // ================================
    List<EventHistoryDto> findVolunteerEventHistory(int volunteerId, int offset, int limit);

    long countVolunteerEventHistory(int volunteerId);

    // ================================
    // 2. VOLUNTEER FEEDBACK CHECKS
    // ================================
    boolean canVolunteerGiveFeedback(int oppId, int volunteerId);

    Feedback findVolunteerFeedback(int oppId, int volunteerId);

    List<Feedback> findByOpportunity(int oppId);

    // ================================
    // 3. CRUD FEEDBACK
    // ================================
    Feedback findById(int id);

    void save(Feedback feedback);

    void update(Feedback feedback);
}
