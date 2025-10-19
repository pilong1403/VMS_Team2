package com.fptuni.vms.integrations.mail;

import org.thymeleaf.context.Context;

public interface MailService {

    /** Gửi mail text thuần */
    void send(String to, String subject, String textBody);

    /** Gửi mail HTML */
    void sendHtml(String to, String subject, String htmlBody);

    /** Gửi mail template (Thymeleaf) */
    void sendTemplate(String to, String subject, String templateName, Context ctx);
}
