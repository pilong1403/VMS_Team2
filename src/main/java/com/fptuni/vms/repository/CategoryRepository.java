// src/main/java/com/fptuni/vms/repository/CategoryRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAll();
    Optional<Category> findById(int id);

    // Tuỳ nhu cầu, có thể cần CRUD đầy đủ:
    Category save(Category c);
    boolean deleteById(int id);
}
