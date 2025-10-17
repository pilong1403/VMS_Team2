// src/main/java/com/fptuni/vms/repository/RoleRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}
