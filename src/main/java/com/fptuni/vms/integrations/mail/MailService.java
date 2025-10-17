// src/main/java/com/fptuni/vms/integrations/mail/MailService.java
package com.fptuni.vms.integrations.mail;

public interface MailService {
    // Đang có
    void send(String to, String subject, String textBody);

    // Thêm mới: gửi HTML thuần
    void sendHtml(String to, String subject, String htmlBody);

    // (tuỳ chọn) Gửi template theo tên + biến
    void sendTemplate(String to, String subject, String templateName, org.thymeleaf.context.Context ctx);
}
