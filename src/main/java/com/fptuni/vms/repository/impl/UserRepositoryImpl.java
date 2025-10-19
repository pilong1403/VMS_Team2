package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Repository
@Transactional

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

    @Override
    public void save1(User user) {
        if (user.getUserId() == null) {
            em.persist(user);
        } else {
            em.merge(user);
        }
    }



    @Override
    public void deleteById(Integer id) {
        User u = em.find(User.class, id);
        if (u != null) em.remove(u);
    }

    @Override
    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
                .getResultList();
    }


    @Override
    public List<User> search(
            String keyword,
            Integer roleId,
            User.UserStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortField,
            String sortDir
    ) {
        List<String> allowedFields = List.of("fullName", "createdAt", "email", "phone");
        if (!allowedFields.contains(sortField)) sortField = "createdAt";
        String direction = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(u.fullName) LIKE LOWER(:kw) " +
                    "OR LOWER(u.email) LIKE LOWER(:kw) " +
                    "OR LOWER(u.phone) LIKE LOWER(:kw))");
            params.put("kw", "%" + keyword.trim() + "%");
        }
        if (roleId != null) {
            jpql.append(" AND u.role.roleId = :roleId");
            params.put("roleId", roleId);
        }
        if (status != null) {
            jpql.append(" AND u.status = :status");
            params.put("status", status);
        }
        if (fromDate != null) {
            jpql.append(" AND u.createdAt >= :fromDt");
            params.put("fromDt", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            // < ngày kế tiếp để bao gồm trọn ngày toDate
            jpql.append(" AND u.createdAt < :toDt");
            params.put("toDt", toDate.plusDays(1).atStartOfDay());
        }

        jpql.append(" ORDER BY u.").append(sortField).append(" ").append(direction);

        TypedQuery<User> query = em.createQuery(jpql.toString(), User.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public long countFiltered(String keyword, Integer roleId, User.UserStatus status,
                              LocalDate fromDate, LocalDate toDate) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(u) FROM User u WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(u.fullName) LIKE LOWER(:kw) " +
                    "OR LOWER(u.email) LIKE LOWER(:kw) " +
                    "OR LOWER(u.phone) LIKE LOWER(:kw))");
            params.put("kw", "%" + keyword.trim() + "%");
        }
        if (roleId != null) {
            jpql.append(" AND u.role.roleId = :roleId");
            params.put("roleId", roleId);
        }
        if (status != null) {
            jpql.append(" AND u.status = :status");
            params.put("status", status);
        }
        if (fromDate != null) {
            jpql.append(" AND u.createdAt >= :fromDt");
            params.put("fromDt", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            jpql.append(" AND u.createdAt < :toDt");
            params.put("toDt", toDate.plusDays(1).atStartOfDay());
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public long countAll() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
    }

    @Override
    public long countByStatus(String status) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

//    @Override
//    public boolean existsByEmail(String email) {
//        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
//                .setParameter("email", email)
//                .getSingleResult();
//        return count > 0;
//    }

    @Override
    public boolean existsByPhone(String phone) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.phone = :phone", Long.class)
                .setParameter("phone", phone)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public List<User> getUsersByRole(Integer roleId) {
        return em.createQuery("SELECT u FROM User u WHERE u.role.id = :roleId ORDER BY u.fullName ASC", User.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

//    @Override
//    public User findByEmail(String email) {
//        List<User> users = em.createQuery(
//                        "SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)", User.class)
//                .setParameter("email", email)
//                .setMaxResults(1)
//                .getResultList();
//        return users.isEmpty() ? null : users.get(0);
//    }
}
