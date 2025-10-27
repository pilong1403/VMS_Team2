package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

import java.util.List;

public interface NotificationService {
    void notifyUsers(List<User> users, String title, String message, String type, String linkUrl, Integer createdBy, Integer orgId);
}
