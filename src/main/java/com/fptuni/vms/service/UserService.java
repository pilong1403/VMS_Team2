package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

import java.util.List;

public interface UserService {

    // ===== CRUD =====
    void saveUser(User user);
    User getUserById(Integer id);
    void deleteUser(Integer id);
    List<User> getAllUsers();

    // ===== SEARCH + FILTER + PAGINATION =====
    List<User> searchUsers(String keyword,
                           Integer roleId,
                           int page,
                           int size,
                           String sortDir);

    long countFilteredUsers(String keyword, Integer roleId);

    // ===== STATISTICS =====
    long countAllUsers();
    long countUsersByStatus(String status);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
