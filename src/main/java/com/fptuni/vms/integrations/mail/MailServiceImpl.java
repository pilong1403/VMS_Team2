package com.fptuni.vms.integrations.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // Đọc từ application.properties (nếu không có thì fallback mặc định)
    @Value("${app.mail.from:no-reply@vms.local}")
    private String fromEmail;

    @Value("${app.mail.from-name:VMS}")
    private String fromName;

    public MailServiceImpl(JavaMailSender mailSender,
                           @Qualifier("mailTemplateEngine") TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void send(String to, String subject, String textBody) {
        sendInternal(to, subject, textBody, false);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        sendInternal(to, subject, htmlBody, true);
    }

    @Override
    public void sendTemplate(String to, String subject, String templateName, Context ctx) {
        try {
            String html = templateEngine.process(templateName, ctx);
            sendHtml(to, subject, html);
        } catch (Exception e) {
            throw new RuntimeException("MAIL_TEMPLATE_ERROR", e);
        }
    }

    /** Hàm tiện ích chung cho text & html */
    private void sendInternal(String to, String subject, String content, boolean isHtml) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(content, isHtml);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("MAIL_SEND_ERROR", e);
        }
    }
}
