// src/main/java/com/fptuni/vms/service/CategoryService.java
package com.fptuni.vms.service;

import com.fptuni.vms.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> listAll();
    Optional<Category> get(int id);

    // CRUD tuỳ nhu cầu
    Category save(Category c);
    boolean delete(int id);
}
