package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

public interface UserService {

    // ===== CRUD =====
    void saveUser(User user);
    User getUserById(Integer id);
    void deleteUser(Integer id);
    List<User> getAllUsers();

    List<User> searchUsers(String keyword, Integer roleId,
                           User.UserStatus status,
                           LocalDate fromDate, LocalDate toDate,
                           int page, int size,
                           String sortField, String sortDir);

    long countFilteredUsers(String keyword, Integer roleId,
                            User.UserStatus status,
                            LocalDate fromDate, LocalDate toDate);

    // ===== STATISTICS =====
    long countAllUsers();
    long countUsersByStatus(String status);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    void exportUserToExcel(User user, OutputStream outputStream) throws IOException;
    List<User> getUsersByRole(Integer roleId);
    User findByEmail(String email);

}
