package com.fptuni.vms.repository;

import com.fptuni.vms.model.Role;

import java.util.Optional;
import java.util.List;


public interface RoleRepository {
    Optional<Role> findByRoleName(String roleName);
    List<Role> findAll();
    Role findById(int id);
}
