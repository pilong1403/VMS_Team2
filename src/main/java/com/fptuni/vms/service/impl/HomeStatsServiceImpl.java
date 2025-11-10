package com.fptuni.vms.service.impl;

import com.fptuni.vms.repository.FeedbackRepository;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.HomeStatsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HomeStatsServiceImpl implements HomeStatsService {

    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final OrganizationRepository organizationRepository;

    public HomeStatsServiceImpl(UserRepository userRepository,
            FeedbackRepository feedbackRepository,
            OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Map<String, Object> getHomeStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Đếm số tình nguyện viên (role_id = 3)
            int volunteerRoleId = userRepository.findRoleIdByName("VOLUNTEER");
            long volunteerCount = userRepository.getUsersByRole(volunteerRoleId).size();
            stats.put("volunteerCount", volunteerCount);
        } catch (Exception e) {
            stats.put("volunteerCount", 0L);
        }

        try {
            // Đếm số cơ hội đã được lan tỏa (tổng số opportunity)
            long opportunitiesCount = organizationRepository.countAll();
            stats.put("opportunitiesCount", opportunitiesCount);
        } catch (Exception e) {
            stats.put("opportunitiesCount", 0L);
        }

        try {
            // Đếm tổng số feedback (thay cho giờ đóng góp)
            long feedbackCount = countAllFeedbacks();
            stats.put("feedbackCount", feedbackCount);
        } catch (Exception e) {
            stats.put("feedbackCount", 0L);
        }

        try {
            // Đếm số tổ chức
            long organizationCount = organizationRepository.countAll();
            stats.put("organizationCount", organizationCount);
        } catch (Exception e) {
            stats.put("organizationCount", 0L);
        }

        return stats;
    }

    /**
     * Đếm tổng số feedback trong hệ thống
     */
    private long countAllFeedbacks() {
        return feedbackRepository.countAll();
    }
}
