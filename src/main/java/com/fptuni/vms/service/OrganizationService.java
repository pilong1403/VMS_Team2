package com.fptuni.vms.service;

import com.fptuni.vms.model.Organization;

import java.time.LocalDate;
import java.util.List;

public interface OrganizationService {
    List<Organization> searchOrganizations(String keyword, Organization.RegStatus status,
                                           LocalDate fromDate, LocalDate toDate,
                                           int page, int size, String sortDir, String sortField);

    long countFiltered(String keyword, Organization.RegStatus status,
                       LocalDate fromDate, LocalDate toDate);

    long countAll();

    Organization getOrganizationById(Integer id);

    void saveOrganization(Organization organization);
}
