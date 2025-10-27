package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.repository.OpportunitySectionRepository;
import com.fptuni.vms.service.OpportunitySectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OpportunitySectionServiceImpl implements OpportunitySectionService {
    private final OpportunitySectionRepository repo;

    public OpportunitySectionServiceImpl(OpportunitySectionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void replaceSections(Opportunity opp, List<OpportunitySection> sections) {
        repo.deleteByOpportunityId(opp.getOppId());
        for (OpportunitySection s : sections) {
            s.setOpportunity(opp);
            repo.save(s);
        }
    }

    @Override
    public List<OpportunitySection> findByOpportunity(Integer oppId) {
        return repo.findByOpportunityId(oppId);
    }
}
