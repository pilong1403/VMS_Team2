package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager em; // container-managed, transaction-scoped

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String normalized = email.trim().toLowerCase();
        TypedQuery<User> q = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
        q.setParameter("email", normalized);
        List<User> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        Long cnt = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", normalized)
                .getSingleResult();
        return cnt != null && cnt > 0;
    }

    @Override
    public Optional<User> findByEmailWithRole(String email) {
        if (email == null) return Optional.empty();
        String normalized = email.trim().toLowerCase();
        TypedQuery<User> q = em.createQuery(
                "SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email", User.class);
        q.setParameter("email", normalized);
        List<User> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<User> findByIdWithRole(Integer userId) {
        if (userId == null) return Optional.empty();
        TypedQuery<User> q = em.createQuery(
                "SELECT u FROM User u JOIN FETCH u.role WHERE u.userId = :id", User.class);
        q.setParameter("id", userId);
        List<User> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** THÊM: dùng ở UserServiceImpl.findById(...) */
    @Override
    public Optional<User> findById(Integer id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public User save(User user) {
        if (user == null) return null;
        if (user.getUserId() == null || user.getUserId() == 0) {
            em.persist(user);     // INSERT
            return user;
        } else {
            return em.merge(user); // UPDATE
        }
    }
}
