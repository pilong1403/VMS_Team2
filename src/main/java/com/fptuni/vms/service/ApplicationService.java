package com.fptuni.vms.service;

import com.fptuni.vms.model.Application;

public interface ApplicationService {
    Application apply(Integer oppId, Integer volunteerId, String reason);
}
