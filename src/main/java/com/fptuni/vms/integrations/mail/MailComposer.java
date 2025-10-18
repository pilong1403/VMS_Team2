package com.fptuni.vms.integrations.mail;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MailComposer {

    private final MailService mailService;
    private final MailTemplateRenderer renderer;

    public MailComposer(MailService mailService, MailTemplateRenderer renderer) {
        this.mailService = mailService;
        this.renderer = renderer;
    }

    public void sendTemplateHtml(String to, String subject, MailTemplates tpl, Map<String, Object> model) {
        String html = renderer.render(tpl, model);
        mailService.sendHtml(to, subject, html);
    }
}
