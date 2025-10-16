package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional
public class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void save(User user) {
        if (user.getUserId() == null) {
            em.persist(user);
        } else {
            em.merge(user);
        }
    }

    @Override
    public User findById(Integer id) {
        return em.find(User.class, id);
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

    @Override
    public boolean existsByEmail(String email) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.phone = :phone", Long.class)
                .setParameter("phone", phone)
                .getSingleResult();
        return count > 0;
    }
}
