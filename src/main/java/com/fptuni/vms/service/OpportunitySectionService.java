package com.fptuni.vms.service;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;

import java.util.List;

public interface OpportunitySectionService {
    List<OpportunitySection> listByOpportunity(Integer oppId);
    void replaceForOpportunity(Opportunity opp, List<OpportunityForm.SectionForm> forms);
    void deleteSection(Integer sectionId);
}
