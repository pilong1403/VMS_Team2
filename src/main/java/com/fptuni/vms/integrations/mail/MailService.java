package com.fptuni.vms.integrations.mail;

import org.thymeleaf.context.Context;

public interface MailService {

    /** Gửi mail text thuần */
    void send(String to, String subject, String textBody);

    /** Gửi mail HTML */
    void sendHtml(String to, String subject, String htmlBody);

    /** Gửi mail template (Thymeleaf) */
    void sendTemplate(String to, String subject, String templateName, Context ctx);
    /** Gửi email khi hồ sơ bị từ chối */
    void sendRejectEmail(String to, String orgName, String reason);

    /** Gửi email khi hồ sơ được duyệt */
    void sendApproveEmail(String to, String orgName, String note);
}
