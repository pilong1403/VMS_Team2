// src/main/java/com/fptuni/vms/repository/OrganizationRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;

import java.util.Optional;

public interface OrganizationRepository {
    Optional<Organization> findByOwnerId(Integer ownerId);

    /** true nếu owner đã có 1 tổ chức (ràng buộc UQ_org_owner) */
    boolean existsByOwner(User owner);

    /** Lưu tổ chức (chỉ cần INSERT cho ca đăng ký) */
    Organization save(Organization org);
}
