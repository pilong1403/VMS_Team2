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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository orgs) {
        this.organizationRepository = orgs;
    }

//    @Override
//    public Organization submitRegistration(User owner, String name, String description,
//                                           String regDocUrl, String note) throws OrgException {
//        if (owner == null) throw new OrgException("NOT_AUTHENTICATED");
//        Role role = owner.getRole();
//        if (role == null || !"ORG_OWNER".equalsIgnoreCase(role.getRoleName())) {
//            throw new OrgException("OWNER_ROLE_REQUIRED");
//        }
//        if (organizationRepository.existsByOwner(owner)) {
//            throw new OrgException("OWNER_ALREADY_HAS_ORG");
//        }
//
//        Organization o = new Organization();
//        o.setOwner(owner);
//        o.setName(name);
//        o.setDescription(description);
//        o.setRegDocUrl(regDocUrl);
//        o.setRegNote(note);
//        o.setRegStatus(Organization.RegStatus.PENDING);
//        o.setRegSubmittedAt(LocalDateTime.now());
//
//        try {
//            return organizationRepository.save(o);
//        } catch (DataIntegrityViolationException e) {
//            // ví dụ: vi phạm UQ_org_owner
//            throw new OrgException("CONSTRAINT_VIOLATION");
//        }
//    }


    @Override
    public List<Organization> searchOrganizations(String keyword, Organization.RegStatus status,
                                                  LocalDate fromDate, LocalDate toDate,
                                                  int page, int size, String sortDir, String sortField) {
        return organizationRepository.search(keyword, status, fromDate, toDate, page, size, sortDir, sortField);
    }

    @Override
    public long countFiltered(String keyword, Organization.RegStatus status,
                              LocalDate fromDate, LocalDate toDate) {
        return organizationRepository.countFiltered(keyword, status, fromDate, toDate);
    }



    @Override
    public long countAll() {
        return organizationRepository.countAll();
    }

    @Override
    public Organization getOrganizationById(Integer id) {
        return organizationRepository.findById(id);
    }

    @Override
    public void saveOrganization(Organization organization) {
        organizationRepository.save(organization);
    }

    @Override
    public List<Organization> getOrganizationByAPPROVED() {
        return organizationRepository.getOrganizationByAPPROVED();

    }

    @Override
    public Organization findByOwnerId(Integer ownerId) {
        return null;
    }

//    @Override
//    public Organization findByOwnerId(Integer ownerId) {
//        return organizationRepository.findByOwnerId(ownerId);
//    }
}
