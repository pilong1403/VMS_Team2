package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.SupportResponse;
import com.fptuni.vms.repository.SupportResponseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Repository
@Transactional
public class SupportResponseRepositoryImpl implements SupportResponseRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 1. Lấy danh sách response có phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public List<SupportResponse> findAllWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        TypedQuery<SupportResponse> query = em.createQuery(
                "SELECT r FROM SupportResponse r",
                SupportResponse.class
        );
        query.setFirstResult(offset);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * 2. Lấy tất cả response
     */
    @Override
    @Transactional(readOnly = true)
    public List<SupportResponse> findAll() {
        return em.createQuery(
                "SELECT r FROM SupportResponse r ORDER BY r.createdAt DESC",
                SupportResponse.class
        ).getResultList();
    }

    /**
     * 3. Thêm một response mới
     */
    @Override
    public SupportResponse save(SupportResponse response) {
        if (response.getResponseId() == null) {
            em.persist(response);
            return response;
        } else {
            return em.merge(response);
        }
    }

    /**
     * 4. Tìm các response theo ticket ID
     */
    @Override
    @Transactional(readOnly = true)
    public List<SupportResponse> findByTicketId(Integer ticketId) {
        TypedQuery<SupportResponse> query = em.createQuery(
                "SELECT r FROM SupportResponse r WHERE r.ticket.ticketId = :ticketId ORDER BY r.createdAt ASC",
                SupportResponse.class
        );
        query.setParameter("ticketId", ticketId);
        return query.getResultList();
    }

    /**
     * 5. Kiểm tra xem ticket có response nào chưa
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasResponses(Integer ticketId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(r) FROM SupportResponse r WHERE r.ticket.ticketId = :ticketId",
                Long.class
        );
        query.setParameter("ticketId", ticketId);
        return query.getSingleResult() > 0;
    }


    @Override
    @Transactional(readOnly = true)
    public List<SupportResponse> filterResponses(String keyword, Integer num, int page, int size) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM SupportResponse r JOIN r.ticket t WHERE 1=1");

        if (StringUtils.hasText(keyword)) {
            jpql.append(" AND (LOWER(t.subject) LIKE :kw OR CAST(t.ticketId AS string) LIKE :kw)");
        }

        TypedQuery<SupportResponse> query = em.createQuery(jpql.toString(), SupportResponse.class);

        if (StringUtils.hasText(keyword)) {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }

        int recordsPerPage = (num != null && num > 0) ? num : size;
        int offset = (page - 1) * recordsPerPage;
        query.setFirstResult(offset);
        query.setMaxResults(recordsPerPage);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFilteredResponses(String keyword) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(DISTINCT r) FROM SupportResponse r JOIN r.ticket t WHERE 1=1");

        if (StringUtils.hasText(keyword)) {
            jpql.append(" AND (LOWER(t.subject) LIKE :kw OR CAST(t.ticketId AS string) LIKE :kw)");
        }
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        if (StringUtils.hasText(keyword)) {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
        return query.getSingleResult();
    }

}
