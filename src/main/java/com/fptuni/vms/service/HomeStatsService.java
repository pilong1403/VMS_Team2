package com.fptuni.vms.service;

import java.util.Map;

public interface HomeStatsService {

    /**
     * Lấy thống kê cho trang home
     * 
     * @return Map chứa các thống kê:
     *         - volunteerCount: Số lượng tình nguyện viên
     *         - opportunitiesCount: Số lượng cơ hội
     *         - feedbackCount: Số lượng feedback
     *         - organizationCount: Số lượng tổ chức
     */
    Map<String, Object> getHomeStats();
}
