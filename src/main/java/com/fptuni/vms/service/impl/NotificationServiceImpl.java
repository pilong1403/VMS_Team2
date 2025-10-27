package com.fptuni.vms.service.impl;

import com.fptuni.vms.integrations.mail.MailService;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.NotificationRepository;
import com.fptuni.vms.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   MailService mailService) {
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public void notifyUsers(List<User> users, String title, String message, String type,
                            String linkUrl, Integer createdBy, Integer orgId) {
        if (users == null || users.isEmpty()) return;

        for (User u : users) {
            // 1) Lưu notification
            notificationRepository.insert(
                    u.getUserId(),
                    message,
                    type, // INFO / ALERT / SYSTEM
                    title,
                    linkUrl,
                    createdBy,
                    orgId
            );

            // 2) Gửi email
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                try {
                    StringBuilder emailBody = new StringBuilder();
                    emailBody.append("<p>Xin chào <b>")
                            .append(u.getFullName() != null ? u.getFullName() : "bạn")
                            .append("</b>,</p>");
                    emailBody.append("<p>").append(message).append("</p>");
                    if (linkUrl != null && !linkUrl.isBlank()) {
                        emailBody.append("<p><a href=\"")
                                .append(linkUrl)
                                .append("\">Xem chi tiết tại đây</a></p>");
                    }
                    emailBody.append("<p>Trân trọng,<br>Ban tổ chức Volunteer Management System.</p>");

                    mailService.sendHtml(u.getEmail(), title, emailBody.toString());
                } catch (Exception e) {
                    System.err.println("Email gửi thất bại tới " + u.getEmail() + ": " + e.getMessage());
                }
            }
        }
    }
}
