package com.fptuni.vms.repository;

import com.fptuni.vms.model.User;
import java.util.List;

public interface UserRepository {

    // ===== CRUD =====
    void save(User user);
    User findById(Integer id);
    void deleteById(Integer id);
    List<User> findAll();

    // ===== SEARCH + FILTER + PAGINATION =====
    List<User> search(String keyword,
                      Integer roleId,
                      int page,
                      int size,
                      String sortDir);
    // keyword: tìm theo tên, email, sđt, địa chỉ
    // roleId: lọc theo vai trò (có thể null)
    // page: số trang (0-based)
    // size: số bản ghi / trang
    // sortDir: "ASC" hoặc "DESC" theo created_at

    // Đếm số kết quả phù hợp
    long countFiltered(String keyword, Integer roleId);

    // ===== STATISTICS =====
    long countAll();
    long countByStatus(String status);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
