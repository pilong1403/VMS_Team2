package com.fptuni.vms.service;

import com.fptuni.vms.model.Opportunity;

import java.util.List;

public interface OpportunityService {
    List<Opportunity> getAll();
    List<Opportunity> findByOrganization(int orgId);
    Opportunity findById(int id);
}
