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
    public void notifyUsers(List<User> users, String title, String message, String type, String linkUrl, Integer createdBy, Integer orgId) {
        if (users == null || users.isEmpty()) return;

        // nội dung email (kèm link nếu có)
        String emailBody = (message == null ? "" : message) +
                (linkUrl != null && !linkUrl.isBlank() ? ("\n\nXem chi tiết: " + linkUrl) : "");

        for (User u : users) {
            // 1) lưu notification
            notificationRepository.insert(
                    u.getUserId(),
                    message,
                    type,       // INFO / ALERT / SYSTEM
                    title,
                    linkUrl,
                    createdBy,
                    orgId
            );

            // 2) gửi email
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                mailService.send(u.getEmail(), title, emailBody); // text thuần là đủ
                // Nếu muốn HTML: mailService.sendHtml(...), hoặc template: mailService.sendTemplate(...)
            }
        }
    }
}
