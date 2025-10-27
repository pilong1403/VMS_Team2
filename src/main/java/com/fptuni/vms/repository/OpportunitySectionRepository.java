package com.fptuni.vms.repository;

import com.fptuni.vms.model.OpportunitySection;
import java.util.List;

public interface OpportunitySectionRepository {
    OpportunitySection save(OpportunitySection s);
    void deleteByOpportunityId(Integer oppId);
    List<OpportunitySection> findByOpportunityId(Integer oppId);
}
