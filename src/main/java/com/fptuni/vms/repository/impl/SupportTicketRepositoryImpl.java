package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.repository.SupportTicketRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Transactional
public class SupportTicketRepositoryImpl implements SupportTicketRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findAll() {
        return em.createQuery(
                "SELECT t FROM SupportTicket t ORDER BY t.ticketId DESC",
                SupportTicket.class
        ).getResultList();
    }

    @Override
    public SupportTicket update(SupportTicket ticket) {
        return em.merge(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findAllWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        TypedQuery<SupportTicket> query = em.createQuery("SELECT t FROM SupportTicket t", SupportTicket.class);
        query.setFirstResult(offset);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicket> findById(Integer id) {
        return Optional.ofNullable(em.find(SupportTicket.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> filterTickets(String status, String priority, Integer num, String keyword, int page, int size) {
        StringBuilder jpql = new StringBuilder("SELECT t FROM SupportTicket t WHERE 1=1");

        // Status (enum)
        SupportTicket.TicketStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = SupportTicket.TicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                jpql.append(" AND t.status = :statusVal");
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        // Priority (enum)
        SupportTicket.TicketPriority priorityEnum = null;
        if (priority != null && !priority.trim().isEmpty()) {
            try {
                priorityEnum = SupportTicket.TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
                jpql.append(" AND t.priority = :priorityVal");
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        // Keyword on id/email/subject
        Integer idExact = null;
        String kw = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmed = keyword.trim();
            // try numeric id
            try {
                idExact = Integer.valueOf(trimmed);
            } catch (NumberFormatException ignored) {
                // not a number
            }
            kw = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";

            jpql.append(" AND (");
            jpql.append(" function('str', t.ticketId) LIKE :kw");
            jpql.append(" OR LOWER(t.contactEmail) LIKE :kw");
            jpql.append(" OR LOWER(t.subject) LIKE :kw");
            if (idExact != null) {
                jpql.append(" OR t.ticketId = :idExact");
            }
            jpql.append(")");
        }

        // Build query
        TypedQuery<SupportTicket> query = em.createQuery(jpql.toString(), SupportTicket.class);

        if (statusEnum != null) {
            query.setParameter("statusVal", statusEnum);
        }
        if (priorityEnum != null) {
            query.setParameter("priorityVal", priorityEnum);
        }
        if (kw != null) {
            query.setParameter("kw", kw);
        }
        if (idExact != null) {
            query.setParameter("idExact", idExact);
        }

        int recordsPerPage = (num != null && num > 0) ? num : size;
//        int offset = Math.max(0, (page - 1) * recordsPerPage);
        int offset = (page - 1) * recordsPerPage;
        query.setFirstResult(offset);
        query.setMaxResults(recordsPerPage);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFilteredTickets(String status, String priority, String keyword) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(t) FROM SupportTicket t WHERE 1=1");

        // Status (enum)
        SupportTicket.TicketStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = SupportTicket.TicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                jpql.append(" AND t.status = :statusVal");
            } catch (IllegalArgumentException ignored) { }
        }

        // Priority (enum)
        SupportTicket.TicketPriority priorityEnum = null;
        if (priority != null && !priority.trim().isEmpty()) {
            try {
                priorityEnum = SupportTicket.TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
                jpql.append(" AND t.priority = :priorityVal");
            } catch (IllegalArgumentException ignored) { }
        }

        // Keyword on id/email/subject
        Integer idExact = null;
        String kw = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmed = keyword.trim();
            try {
                idExact = Integer.valueOf(trimmed);
            } catch (NumberFormatException ignored) { }
            kw = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";

            jpql.append(" AND (");
            jpql.append(" function('str', t.ticketId) LIKE :kw");
            jpql.append(" OR LOWER(t.contactEmail) LIKE :kw");
            jpql.append(" OR LOWER(t.subject) LIKE :kw");
            if (idExact != null) {
                jpql.append(" OR t.ticketId = :idExact");
            }
            jpql.append(")");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (statusEnum != null) {
            query.setParameter("statusVal", statusEnum);
        }
        if (priorityEnum != null) {
            query.setParameter("priorityVal", priorityEnum);
        }
        if (kw != null) {
            query.setParameter("kw", kw);
        }
        if (idExact != null) {
            query.setParameter("idExact", idExact);
        }

        return query.getSingleResult();
    }
}
