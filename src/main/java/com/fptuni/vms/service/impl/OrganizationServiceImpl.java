// src/main/java/com/fptuni/vms/service/impl/OrganizationServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.service.OrganizationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository orgs;

    public OrganizationServiceImpl(OrganizationRepository orgs) {
        this.orgs = orgs;
    }

    @Override
    public Organization submitRegistration(User owner, String name, String description,
                                           String regDocUrl, String note) throws OrgException {
        if (owner == null) throw new OrgException("NOT_AUTHENTICATED");
        Role role = owner.getRole();
        if (role == null || !"ORG_OWNER".equalsIgnoreCase(role.getRoleName())) {
            throw new OrgException("OWNER_ROLE_REQUIRED");
        }
        if (orgs.existsByOwner(owner)) {
            throw new OrgException("OWNER_ALREADY_HAS_ORG");
        }

        Organization o = new Organization();
        o.setOwner(owner);
        o.setName(name);
        o.setDescription(description);
        o.setRegDocUrl(regDocUrl);
        o.setRegNote(note);
        o.setRegStatus(Organization.RegStatus.PENDING);
        o.setRegSubmittedAt(LocalDateTime.now());

        try {
            return orgs.save(o);
        } catch (DataIntegrityViolationException e) {
            // ví dụ: vi phạm UQ_org_owner
            throw new OrgException("CONSTRAINT_VIOLATION");
        }
    }
}
