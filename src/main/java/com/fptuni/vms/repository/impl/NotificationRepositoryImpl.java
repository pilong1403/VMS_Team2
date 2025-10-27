package com.fptuni.vms.repository.impl;

import com.fptuni.vms.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void insert(Integer userId, String message, String type, String title, String linkUrl,
                       Integer createdBy, Integer orgId) {

        // Viết gộp 1 dòng để IntelliJ nhận đúng toàn bộ query (tránh parse lỗi)
        String sql = "INSERT INTO dbo.notifications " +
                "(user_id, message, type, is_read, title, link_url, created_by, org_id, created_at) " +
                "VALUES (:userId, :message, :type, 0, :title, :link, :createdBy, :orgId, SYSDATETIME())";

        em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("message", message)
                .setParameter("type", type)      // INFO / ALERT / SYSTEM
                .setParameter("title", title)
                .setParameter("link", linkUrl)
                .setParameter("createdBy", createdBy)
                .setParameter("orgId", orgId)
                .executeUpdate();
    }
}
