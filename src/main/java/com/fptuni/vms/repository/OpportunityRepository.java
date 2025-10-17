package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;

import java.util.List;

public interface OpportunityRepository {
    List<Opportunity> getAll();
    List<Opportunity> findByOrganization(int orgId);
    Opportunity findById(int id);

}
