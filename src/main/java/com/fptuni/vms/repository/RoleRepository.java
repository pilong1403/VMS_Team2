package com.fptuni.vms.repository;

import com.fptuni.vms.model.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByRoleName(String roleName);
}
