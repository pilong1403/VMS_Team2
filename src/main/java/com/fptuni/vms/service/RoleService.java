package com.fptuni.vms.service;
import com.fptuni.vms.model.Role;
import java.util.List;

public interface RoleService {
    Role getRoleByName(String name);
    List<Role> getAllRoles();
}
