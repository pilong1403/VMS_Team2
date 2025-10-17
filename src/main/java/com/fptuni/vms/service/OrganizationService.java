// src/main/java/com/fptuni/vms/service/OrganizationService.java
package com.fptuni.vms.service;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;

public interface OrganizationService {
    Organization submitRegistration(User owner, String name, String description,
                                    String regDocUrl, String note) throws OrgException;

    class OrgException extends Exception {
        public OrgException(String code) { super(code); }
    }
}
