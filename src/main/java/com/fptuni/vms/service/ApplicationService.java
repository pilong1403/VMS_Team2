package com.fptuni.vms.service;

import java.util.List;

import com.fptuni.vms.model.Application;

public interface ApplicationService {
    Application applyOpportunity(Integer opportunityId, String email, String reason);

    List<Application> getApplicationsByVolunteerId(Integer userId, String q, String status, String sort);

    Integer getVolunteerIdByEmail(String email);

    Application getApplicationDetail(Integer appId);

    // huỷ đơn ứng tuyển
    void cancelApplication(Integer appId, String reason);
}
