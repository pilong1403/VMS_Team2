package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    // ===== CRUD =====
    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ===== SEARCH + FILTER + PAGINATION =====
    @Override
    public List<User> searchUsers(String keyword, Integer roleId,
                                  int page, int size, String sortDir) {
        return userRepository.search(keyword, roleId, page, size, sortDir);
    }

    @Override
    public long countFilteredUsers(String keyword, Integer roleId) {
        return userRepository.countFiltered(keyword, roleId);
    }

    // ===== STATISTICS =====
    @Override
    public long countAllUsers() {
        return userRepository.countAll();
    }

    @Override
    public long countUsersByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }


}
