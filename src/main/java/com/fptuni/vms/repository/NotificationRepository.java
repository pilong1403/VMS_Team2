package com.fptuni.vms.repository;

public interface NotificationRepository {
    void insert(Integer userId, String message, String type, String title, String linkUrl, Integer createdBy, Integer orgId);
}
