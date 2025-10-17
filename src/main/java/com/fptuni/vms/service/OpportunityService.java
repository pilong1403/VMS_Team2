// src/main/java/com/fptuni/vms/service/OpportunityService.java
package com.fptuni.vms.service;

import com.fptuni.vms.model.Opportunity;

import java.util.List;
import java.util.Optional;

public interface OpportunityService {
    record Page<T>(List<T> items, int total, int page, int size) {}

    Page<Opportunity> listMyOpps(int ownerUserId, int page, int size, String q, Integer categoryId, String status);
    Optional<Opportunity> getMyOppById(int ownerUserId, int oppId);
    Opportunity createMyOpp(int ownerUserId, Opportunity opp);
    Opportunity updateMyOpp(int ownerUserId, Opportunity opp);
    boolean deleteMyOpp(int ownerUserId, int oppId);
}
