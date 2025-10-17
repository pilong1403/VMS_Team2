package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

import java.util.Optional;

public interface UserService {

    /** Lưu/sửa người dùng */
    User save(User user);

    /** Email đã tồn tại chưa (dùng khi đăng ký) */
    boolean existsByEmail(String email);

    /** Tìm theo email (dùng khi đăng nhập) */
    Optional<User> findByEmail(String email);

    /** Tìm theo id (tiện lấy user từ session id) */
    Optional<User> findById(Integer id);
}
