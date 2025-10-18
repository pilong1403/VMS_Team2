package com.fptuni.vms.repository;

import com.fptuni.vms.model.User;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
public interface UserRepository {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailWithRole(String email);

    Optional<User> findByIdWithRole(Integer userId);


    Optional<User> findById(Integer id);

//    User save(User user);
    void save(User user);
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
    boolean existsByPhone(String phone);
    List<User> getUsersByRole(Integer roleId);


}
