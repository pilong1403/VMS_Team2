package com.fptuni.vms.repository;

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
}
