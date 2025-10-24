package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.repository.OpportunitySectionRepository;
import com.fptuni.vms.service.OpportunitySectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OpportunitySectionServiceImpl implements OpportunitySectionService {

    private final OpportunitySectionRepository repo;

    public OpportunitySectionServiceImpl(OpportunitySectionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpportunitySection> listByOpportunity(Integer oppId) {
        return repo.findByOpportunityId(oppId);
    }

    @Override
    public void replaceForOpportunity(Opportunity opp, List<OpportunityForm.SectionForm> forms) {
        repo.deleteAllForOpportunity(opp.getOppId());
        if (forms == null || forms.isEmpty()) return;

        List<OpportunitySection> batch = new ArrayList<>();
        for (OpportunityForm.SectionForm f : forms) {
            if (f.getSectionOrder() == null) continue;
            OpportunitySection s = new OpportunitySection();
            s.setOpportunity(opp);
            s.setSectionOrder(f.getSectionOrder());
            s.setHeading(f.getHeading());
            s.setContent(f.getContent());
            s.setImageUrl(f.getImageUrl());
            s.setCaption(f.getCaption());
            batch.add(s);
        }
        repo.saveAll(batch);
    }

    @Override
    public void deleteSection(Integer sectionId) {
        repo.deleteById(sectionId);
    }
}
