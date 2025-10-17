package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.repository.OpportunityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {
    @PersistenceContext
    EntityManager em;

    @Override
    public List<Opportunity> getAll() {
        return em.createQuery("""
                SELECT o 
                FROM Opportunity o 
                JOIN FETCH o.organization org 
                LEFT JOIN FETCH o.category c
                ORDER BY o.createdAt DESC
                """, Opportunity.class)
                .getResultList();
    }


    @Override
    public List<Opportunity> findByOrganization(int orgId) {
        return em.createQuery("""
                SELECT o 
                FROM Opportunity o 
                JOIN FETCH o.organization org 
                LEFT JOIN FETCH o.category c
                WHERE org.orgId = :orgId
                ORDER BY o.createdAt DESC
                """, Opportunity.class)
                .setParameter("orgId", orgId)
                .getResultList();
    }

    @Override
    public Opportunity findById(int id) {
        try {
            return em.createQuery("""
                    SELECT o
                    FROM Opportunity o
                    JOIN FETCH o.organization org
                    LEFT JOIN FETCH o.category c
                    WHERE o.oppId = :id
                    """, Opportunity.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // tránh exception nếu không tìm thấy
        }
    }
}
