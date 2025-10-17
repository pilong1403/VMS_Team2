// src/main/java/com/fptuni/vms/service/impl/CategoryServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.repository.CategoryRepository;
import com.fptuni.vms.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repo;

    public CategoryServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Category> listAll() {
        return repo.findAll();
    }

    @Override
    public Optional<Category> get(int id) {
        return repo.findById(id);
    }

    @Override
    public Category save(Category c) {
        return repo.save(c);
    }

    @Override
    public boolean delete(int id) {
        return repo.deleteById(id);
    }
}
