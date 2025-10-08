package com.fptuni.vms.repository;

import com.fptuni.vms.model.Role;

import java.util.List;

public interface RoleRepository {
    List<Role> findAll();
    Role findById(int id);
}
