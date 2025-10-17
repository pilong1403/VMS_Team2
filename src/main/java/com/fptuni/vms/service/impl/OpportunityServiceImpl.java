// src/main/java/com/fptuni/vms/service/impl/OpportunityServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.service.OpportunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityRepository oppRepo;
    private final OrganizationRepository orgRepo;

    public OpportunityServiceImpl(OpportunityRepository oppRepo, OrganizationRepository orgRepo) {
        this.oppRepo = oppRepo;
        this.orgRepo = orgRepo;
    }

    private Organization requireMyOrg(int ownerUserId) {
        return orgRepo.findByOwnerId(ownerUserId)
                .orElseThrow(() -> new IllegalStateException("ORG_NOT_FOUND_FOR_OWNER"));
    }

    @Override
    public Page<Opportunity> listMyOpps(int ownerUserId, int page, int size, String q, Integer categoryId, String status) {
        Organization org = requireMyOrg(ownerUserId);
        int offset = Math.max(0, page) * Math.max(1, size);
        int limit = Math.max(1, size);
        var items = oppRepo.findByOrgIdPaged(org.getOrgId(), offset, limit, q, categoryId, status);
        int total = oppRepo.countByOrgId(org.getOrgId(), q, categoryId, status);
        return new Page<>(items, total, page, size);
    }

    @Override
    public Optional<Opportunity> getMyOppById(int ownerUserId, int oppId) {
        Organization org = requireMyOrg(ownerUserId);
        return oppRepo.findByIdAndOrg(oppId, org.getOrgId());
    }

    @Override
    @Transactional
    public Opportunity createMyOpp(int ownerUserId, Opportunity opp) {
        Organization org = requireMyOrg(ownerUserId);
        opp.setOrganization(org);
        if (opp.getStatus() == null) {
            opp.setStatus(Opportunity.OpportunityStatus.OPEN);
        }
        return oppRepo.save(opp);
    }

    @Override
    @Transactional
    public Opportunity updateMyOpp(int ownerUserId, Opportunity opp) {
        Organization org = requireMyOrg(ownerUserId);
        oppRepo.findByIdAndOrg(opp.getOppId(), org.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("OPP_NOT_FOUND_OR_NOT_OWNED"));
        opp.setOrganization(org);
        return oppRepo.save(opp);
    }

    @Override
    @Transactional
    public boolean deleteMyOpp(int ownerUserId, int oppId) {
        Organization org = requireMyOrg(ownerUserId);
        return oppRepo.deleteByIdAndOrg(oppId, org.getOrgId());
    }
}
