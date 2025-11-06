package com.fptuni.vms.repository.impl;



import com.fptuni.vms.repository.ReportRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

    @Repository
    public class ReportRepositoryImpl implements ReportRepository {

        @Autowired
        private SessionFactory sessionFactory;

        @Override
        public List<Object[]> countUsersByDateRange(LocalDate from, LocalDate to) {
            try (Session session = sessionFactory.openSession()) {
                Query<Object[]> query = session.createQuery(
                        "SELECT CAST(u.createdAt AS date), COUNT(u.userId) " +
                                "FROM User u " +
                                "WHERE CAST(u.createdAt AS date) BETWEEN :from AND :to " +
                                "GROUP BY CAST(u.createdAt AS date) " +
                                "ORDER BY CAST(u.createdAt AS date)",
                        Object[].class
                );
                query.setParameter("from", from);
                query.setParameter("to", to);
                return query.list();
            }
        }

        @Override
        public Map<String, Long> countUsersByRole() {
            Map<String, Long> result = new LinkedHashMap<>();
            try (Session session = sessionFactory.openSession()) {
                Query<Object[]> query = session.createQuery(
                        "SELECT u.role.roleName, COUNT(u.userId) FROM User u GROUP BY u.role.roleName",
                        Object[].class
                );
                for (Object[] row : query.list()) {
                    result.put((String) row[0], (Long) row[1]);
                }
            }
            return result;
        }

        @Override
        public List<Object[]> countUsersByWeek(LocalDate from, LocalDate to) {
            try (Session session = sessionFactory.openSession()) {
                Query<Object[]> query = session.createQuery(
                        "SELECT FUNCTION('week', u.createdAt), COUNT(u.userId) " +
                                "FROM User u WHERE CAST(u.createdAt AS date) BETWEEN :from AND :to " +
                                "GROUP BY FUNCTION('week', u.createdAt) " +
                                "ORDER BY FUNCTION('week', u.createdAt)",
                        Object[].class
                );
                query.setParameter("from", from);
                query.setParameter("to", to);
                return query.list();
            }
        }

        @Override
        public List<Object[]> countUsersByMonth(LocalDate from, LocalDate to) {
            try (Session session = sessionFactory.openSession()) {
                Query<Object[]> query = session.createQuery(
                        "SELECT MONTH(u.createdAt), COUNT(u.userId) " +
                                "FROM User u WHERE CAST(u.createdAt AS date) BETWEEN :from AND :to " +
                                "GROUP BY MONTH(u.createdAt) ORDER BY MONTH(u.createdAt)",
                        Object[].class
                );
                query.setParameter("from", from);
                query.setParameter("to", to);
                return query.list();
            }
        }

        @Override
        public Map<String, Long> countUsersByStatus() {
            Map<String, Long> result = new LinkedHashMap<>();
            try (Session session = sessionFactory.openSession()) {
                Query<Object[]> q = session.createQuery(
                        "SELECT u.status, COUNT(u.userId) FROM User u GROUP BY u.status",
                        Object[].class
                );
                for (Object[] row : q.list()) {
                    result.put(String.valueOf(row[0]), (Long) row[1]); // ACTIVE / LOCKED
                }
            }
            return result;
        }

        @Override
        public Long countUsersByRoleName(String roleName) {
            try (Session session = sessionFactory.openSession()) {
                Query<Long> q = session.createQuery(
                        "SELECT COUNT(u.userId) FROM User u WHERE u.role.roleName = :r", Long.class);
                q.setParameter("r", roleName);
                return q.uniqueResult();
            }
        }

        @Override
        public Long countOrganizations() {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("SELECT COUNT(o.orgId) FROM Organization o", Long.class)
                        .uniqueResult();
            }
        }

        @Override
        public Long countOpportunities() {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("SELECT COUNT(op.oppId) FROM Opportunity op", Long.class)
                        .uniqueResult();
            }
        }
        @Override
        public LocalDate findFirstUserCreatedDate() {
            try (Session session = sessionFactory.openSession()) {
                Query<java.sql.Date> q = session.createQuery(
                        "SELECT MIN(CAST(u.createdAt AS date)) FROM User u",
                        java.sql.Date.class
                );
                java.sql.Date d = q.uniqueResult();
                return d != null ? d.toLocalDate() : null;
            }
        }

        @Override
        public LocalDate findLastUserCreatedDate() {
            try (Session session = sessionFactory.openSession()) {
                Query<java.sql.Date> q = session.createQuery(
                        "SELECT MAX(CAST(u.createdAt AS date)) FROM User u",
                        java.sql.Date.class
                );
                java.sql.Date d = q.uniqueResult();
                return d != null ? d.toLocalDate() : null;
            }
        }


    }


