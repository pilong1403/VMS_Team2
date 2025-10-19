package com.fptuni.vms.repository;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

public interface OrganizationRepository {
    Optional<Organization> findByOwnerId(Integer ownerId);

    /** true nếu owner đã có 1 tổ chức (ràng buộc UQ_org_owner) */
    boolean existsByOwner(User owner);

    /** Lưu tổ chức (chỉ cần INSERT cho ca đăng ký) */
    Organization save(Organization org);
    void save1(Organization organization);

    Organization findById(Integer id);

    List<Organization> search(String keyword, Organization.RegStatus status,
                              LocalDate fromDate, LocalDate toDate,
                              int page, int size, String sortDir, String sortField);

    long countFiltered(String keyword, Organization.RegStatus status,
                       LocalDate fromDate, LocalDate toDate);

    long countAll();
    List<Organization> getOrganizationByAPPROVED();
//    Organization findByOwnerId(Integer ownerId);

}
