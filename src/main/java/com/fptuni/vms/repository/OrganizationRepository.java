package com.fptuni.vms.repository;

import com.fptuni.vms.model.Organization;

import java.time.LocalDate;
import java.util.List;

public interface OrganizationRepository {
    void save(Organization organization);

    Organization findById(Integer id);

    List<Organization> search(String keyword, Organization.RegStatus status,
                              LocalDate fromDate, LocalDate toDate,
                              int page, int size, String sortDir, String sortField);

    long countFiltered(String keyword, Organization.RegStatus status,
                       LocalDate fromDate, LocalDate toDate);

    long countAll();
    List<Organization> getOrganizationByAPPROVED();
    Organization findByOwnerId(Integer ownerId);

}
