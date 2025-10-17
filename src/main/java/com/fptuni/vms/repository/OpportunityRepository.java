// src/main/java/com/fptuni/vms/repository/OpportunityRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.Opportunity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OpportunityRepository {
    // đã có gì thì giữ nguyên, thêm:
    List<Opportunity> findByOrgIdPaged(int orgId, int offset, int limit, String q, Integer categoryId, String status);
    int countByOrgId(int orgId, String q, Integer categoryId, String status);
    Optional<Opportunity> findByIdAndOrg(int oppId, int orgId);
    Opportunity save(Opportunity o);
    boolean deleteByIdAndOrg(int oppId, int orgId);

    // tiện lợi:
    List<Opportunity> findRecentByOrg(int orgId, LocalDateTime from, LocalDateTime to);
}
