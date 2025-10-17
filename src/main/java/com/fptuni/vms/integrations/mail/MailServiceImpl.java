// src/main/java/com/fptuni/vms/integrations/mail/MailServiceImpl.java
package com.fptuni.vms.integrations.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine mailTemplateEngine;

    // TODO: đổi thành email thật + cùng domain SMTP của bạn
    private static final String DEFAULT_FROM_EMAIL = "youraccount@gmail.com";
    private static final String DEFAULT_FROM_NAME  = "VMS";

    public MailServiceImpl(JavaMailSender mailSender,
                           @Qualifier("mailTemplateEngine") TemplateEngine mailTemplateEngine) {
        this.mailSender = mailSender;
        this.mailTemplateEngine = mailTemplateEngine;
    }

    @Override
    public void send(String to, String subject, String textBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(new InternetAddress(DEFAULT_FROM_EMAIL, DEFAULT_FROM_NAME, "UTF-8"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(textBody, false);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("MAIL_SEND_ERROR", e);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(new InternetAddress(DEFAULT_FROM_EMAIL, DEFAULT_FROM_NAME, "UTF-8"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("MAIL_SEND_ERROR", e);
        }
    }

    @Override
    public void sendTemplate(String to, String subject, String templateName, Context ctx) {
        try {
            String html = mailTemplateEngine.process(templateName, ctx);
            sendHtml(to, subject, html);
        } catch (Exception e) {
            throw new RuntimeException("MAIL_SEND_ERROR", e);
        }
    }
}
