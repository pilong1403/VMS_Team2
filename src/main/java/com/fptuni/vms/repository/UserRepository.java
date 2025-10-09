package com.fptuni.vms.repository;

import com.fptuni.vms.model.User;

import java.time.LocalDate;
import java.util.List;


public interface UserRepository {
    void save(User user);
    User findById(Integer id);
    void deleteById(Integer id);
    List<User> findAll();

    // ✅ backend search đầy đủ
    List<User> search(
            String keyword,
            Integer roleId,
            User.UserStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortField,
            String sortDir
    );

    long countFiltered(
            String keyword,
            Integer roleId,
            User.UserStatus status,
            LocalDate fromDate,
            LocalDate toDate
    );

    long countAll();
    long countByStatus(String status);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
