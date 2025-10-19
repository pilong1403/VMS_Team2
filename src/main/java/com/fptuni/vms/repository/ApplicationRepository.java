package com.fptuni.vms.repository;

import java.util.List;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;

public interface ApplicationRepository {
        boolean existsByOppIdAndVolunteerId(Integer oppId, Integer volunteerId);

        Application save(Application application);

        Opportunity findOpportunityById(Integer oppId);

        User findUserById(Integer userId);

        // đếm số application hợp lệ của 1 opportunity
        long countByOppId(Integer oppId);

        // cho phép lưu (merge) lại thông tin liên hệ của user
        User saveUser(User user);

        // LẤY DANH SÁCH ĐƠN CỦA VOLUNTEER — trả về Application + fetch join đủ dữ liệu
        List<Application> findAllByVolunteerId(Integer volunteerId);
}
