package com.fptuni.vms.service;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;

import java.util.List;

public interface OpportunitySectionService {
    void replaceSections(Opportunity opp, List<OpportunitySection> sections);
    List<OpportunitySection> findByOpportunity(Integer oppId);
}
