package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Role;
import com.fptuni.vms.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    @PersistenceContext
    private EntityManager em; // container-managed

    @Override
    public Optional<Role> findByRoleName(String roleName) {
        if (roleName == null) return Optional.empty();

        // Chuẩn hóa giống @PrePersist/@PreUpdate của entity: trim + UPPER
        String normalized = roleName.trim().toUpperCase();

        TypedQuery<Role> q = em.createQuery(
                "SELECT r FROM Role r WHERE r.roleName = :name", Role.class);
        q.setParameter("name", normalized);

        List<Role> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
    @Override
    public List<Role> findAll() {
        return em.createQuery("SELECT r FROM Role r", Role.class).getResultList();
    }

    @Override
    public Role findById(int id) {
        return null;
    }
}
