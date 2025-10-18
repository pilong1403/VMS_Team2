package com.fptuni.vms.repository;

import com.fptuni.vms.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailWithRole(String email);

    Optional<User> findByIdWithRole(Integer userId);


    Optional<User> findById(Integer id);

    User save(User user);
}
