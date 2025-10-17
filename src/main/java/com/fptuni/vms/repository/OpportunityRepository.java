package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Integer> {
    Optional<Opportunity> findByOppIdAndStatus(Integer oppId, String status);
}
