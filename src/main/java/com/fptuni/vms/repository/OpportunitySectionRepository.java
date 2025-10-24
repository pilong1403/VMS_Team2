package com.fptuni.vms.repository;

import com.fptuni.vms.model.OpportunitySection;

import java.util.List;
import java.util.Optional;

public interface OpportunitySectionRepository {
    List<OpportunitySection> findByOpportunityId(Integer oppId);
    OpportunitySection save(OpportunitySection s);
    void saveAll(List<OpportunitySection> items);
    void deleteById(Integer sectionId);
    void deleteAllForOpportunity(Integer oppId);
    Optional<OpportunitySection> findById(Integer sectionId);
}
