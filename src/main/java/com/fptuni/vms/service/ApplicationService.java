package com.fptuni.vms.service;

import com.fptuni.vms.model.Application;
import java.util.List;
import java.util.Optional;

public interface ApplicationService {

    List<Application> getAllApplications();
    Optional<Application> getApplicationById(Integer appId);

    List<Application> getApplicationsByVolunteer(Integer volunteerId);
    List<Application> getApplicationsByOpportunity(Integer oppId);

    Application applyForOpportunity(Application application);
    boolean cancelApplication(Integer appId, String cancelReason);
    List<Application> getVolunteersByOpportunity(Integer oppId);

}
