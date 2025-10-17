package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.service.OpportunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
public class OpportunityServiceImpl implements OpportunityService {
    @Autowired
    private OpportunityRepository repo;

    @Override public List<Opportunity> getAll() { return repo.getAll(); }

    @Override
    public List<Opportunity> findByOrganization(int orgId) {
        return repo.findByOrganization(orgId);
    }

    @Override
    public Opportunity findById(int id) {
        return repo.findById(id);
    }
}
