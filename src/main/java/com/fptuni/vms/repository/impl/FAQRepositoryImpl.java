package com.fptuni.vms.repository.impl;
import com.fptuni.vms.model.FAQ;
import com.fptuni.vms.repository.FAQRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class FAQRepositoryImpl implements FAQRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<FAQ> findAllWithoutPagination() {
        TypedQuery<FAQ> query = em.createQuery("SELECT f FROM FAQ f", FAQ.class);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FAQ> findAll(int page, int size) {
        int offset = (page - 1) * size;
        TypedQuery<FAQ> query = em.createQuery("SELECT f FROM FAQ f", FAQ.class);
        query.setFirstResult(offset);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FAQ> findById(Integer id) {
        return Optional.ofNullable(em.find(FAQ.class, id));
    }

    @Override
    public FAQ create(FAQ faq) {
        em.persist(faq);
        return faq;
    }

    @Override
    public FAQ update(FAQ faq) {
        if (faq.getFaqId() == null) {
            throw new IllegalArgumentException("FAQ id must not be null for update");
        }
        return em.merge(faq);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Integer id) {
        Long count = em.createQuery("SELECT COUNT(f) FROM FAQ f WHERE f.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return em.createQuery("SELECT COUNT(f) FROM FAQ f", Long.class).getSingleResult();
    }


    @Override
    @Transactional(readOnly = true)
    public FAQ findByQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return null;
        }
        String normalized = question.trim().toLowerCase();
        List<FAQ> results = em.createQuery(
                        "SELECT f FROM FAQ f WHERE LOWER(f.question) = :q", FAQ.class)
                .setParameter("q", normalized)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public FAQ findDuplicateQuestionOnUpdate(String question, Integer currentFaqId) {
        if (question == null || question.trim().isEmpty()) {
            return null;
        }

        String normalized = question.trim().toLowerCase();
        List<FAQ> results = em.createQuery(
                        "SELECT f FROM FAQ f WHERE LOWER(f.question) = :q AND f.faqId <> :id", FAQ.class)
                .setParameter("q", normalized)
                .setParameter("id", currentFaqId)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }


    @Override
    @Transactional(readOnly = true)
    public List<FAQ> filterFAQs(String status, String category, Integer num, String keyword, int page, int size) {
        String vietnameseCategory = null;
        if (category != null && !category.trim().isEmpty()) {
            switch (category.trim().toLowerCase()) {
                case "account":
                    vietnameseCategory = "Tài khoản";
                    break;
                case "event":
                    vietnameseCategory = "Sự kiện";
                    break;
                case "org":
                    vietnameseCategory = "Tổ chức";
                    break;
                case "donation":
                    vietnameseCategory = "Quyên góp";
                    break;
                case "common":
                    vietnameseCategory = "Chung";
                    break;
            }
        }

        StringBuilder jpql = new StringBuilder("SELECT f FROM FAQ f WHERE 1=1");

        if (vietnameseCategory != null) {
            jpql.append(" AND f.category = :categoryValue");
        }

        if (status != null && !status.trim().isEmpty()) {
            if ("0".equals(status) || "1".equals(status)) {
                jpql.append(" AND f.status = :statusValue");
            }
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append(" AND LOWER(f.question) LIKE LOWER(:kw)");
        }

        TypedQuery<FAQ> query = em.createQuery(jpql.toString(), FAQ.class);

        if (vietnameseCategory != null) {
            query.setParameter("categoryValue", vietnameseCategory);
        }

        if (status != null && ("0".equals(status) || "1".equals(status))) {
            boolean statusAsBoolean = "1".equals(status);
            query.setParameter("statusValue", statusAsBoolean);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        int recordsPerPage = (num != null && num > 0) ? num : size;
        int offset = (page - 1) * recordsPerPage;
        query.setFirstResult(offset);
        query.setMaxResults(recordsPerPage);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFilteredFAQs(String status, String category, String keyword) {
        String vietnameseCategory = null;
        if (category != null && !category.trim().isEmpty()) {
            switch (category.trim().toLowerCase()) {
                case "account":
                    vietnameseCategory = "Tài khoản";
                    break;
                case "event":
                    vietnameseCategory = "Sự kiện";
                    break;
                case "org":
                    vietnameseCategory = "Tổ chức";
                    break;
                case "donation":
                    vietnameseCategory = "Quyên góp";
                    break;
                case "common":
                    vietnameseCategory = "Chung";
                    break;
            }
        }

        StringBuilder jpql = new StringBuilder("SELECT COUNT(f.id) FROM FAQ f WHERE 1=1");

        if (vietnameseCategory != null) {
            jpql.append(" AND f.category = :categoryValue");
        }

        if (status != null && !status.trim().isEmpty()) {
            if ("0".equals(status) || "1".equals(status)) {
                jpql.append(" AND f.status = :statusValue");
            }
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append(" AND LOWER(f.question) LIKE LOWER(:kw)");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (vietnameseCategory != null) {
            query.setParameter("categoryValue", vietnameseCategory);
        }

        if (status != null && ("0".equals(status) || "1".equals(status))) {
            boolean statusAsBoolean = "1".equals(status);
            query.setParameter("statusValue", statusAsBoolean);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }

        return query.getSingleResult();
    }
}
