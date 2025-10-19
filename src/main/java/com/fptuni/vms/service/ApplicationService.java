package com.fptuni.vms.service;

import java.util.List;

import com.fptuni.vms.model.Application;

public interface ApplicationService {
    Application apply(Integer oppId, Integer volunteerId, String reason);

    // apply kèm cập nhật nhanh thông tin liên hệ
    Application apply(Integer oppId, Integer volunteerId, String reason,
            String fullName, String phone, String address);

    // danh sách đơn của volunteer
    List<Application> listMyApplications(Integer volunteerId);
}
