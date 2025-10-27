package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.OpportunitySection;
import com.fptuni.vms.repository.OpportunitySectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OpportunitySectionRepositoryImpl implements OpportunitySectionRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public OpportunitySection save(OpportunitySection s) {
        if (s.getSectionId() == null) {
            em.persist(s);
            return s;
        }
        return em.merge(s);
    }

    @Override
    public void deleteByOpportunityId(Integer oppId) {
        em.createQuery("DELETE FROM OpportunitySection s WHERE s.opportunity.oppId = :id")
                .setParameter("id", oppId)
                .executeUpdate();
    }

    @Override
    public List<OpportunitySection> findByOpportunityId(Integer oppId) {
        return em.createQuery("""
                SELECT s FROM OpportunitySection s
                WHERE s.opportunity.oppId = :id
                ORDER BY s.sectionOrder ASC
                """, OpportunitySection.class)
                .setParameter("id", oppId)
                .getResultList();
    }
}
