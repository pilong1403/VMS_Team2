package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.OrganizationRepository;
import com.fptuni.vms.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {
    @Autowired
    private OrganizationRepository organizationRepository;

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
}
