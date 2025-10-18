// src/main/java/com/fptuni/vms/repository/impl/CategoryRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    @PersistenceContext
    private EntityManager em; // container-managed, transaction-scoped

    @Override
    public List<Category> findAll() {
        TypedQuery<Category> q = em.createQuery(
                "SELECT c FROM Category c ORDER BY c.categoryName ASC", Category.class);
        return q.getResultList();
    }

    @Override
    public Optional<Category> findById(int id) {
        return Optional.ofNullable(em.find(Category.class, id));
    }

    @Override
    public Category save(Category c) {
        if (c == null) return null;

        // Insert nếu chưa có id; update nếu đã có id
        if (c.getCategoryId() == null || c.getCategoryId() == 0) {
            em.persist(c);   // ID sẽ có sau flush (IDENTITY)
            return c;
        } else {
            return em.merge(c);
        }
    }

    @Override
    public boolean deleteById(int id) {
        Category found = em.find(Category.class, id);
        if (found == null) return false;
        em.remove(found);
        return true;
    }
}
