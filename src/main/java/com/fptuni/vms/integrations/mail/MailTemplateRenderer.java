// src/main/java/com/fptuni/vms/integrations/mail/MailTemplateRenderer.java
package com.fptuni.vms.integrations.mail;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class MailTemplateRenderer {

    private final TemplateEngine templateEngine;

    public MailTemplateRenderer(@Qualifier("mailTemplateEngine") TemplateEngine mailTemplateEngine) {
        this.templateEngine = mailTemplateEngine;
    }

    public String render(MailTemplates tpl, Map<String, Object> model) {
        Context ctx = new Context();
        if (model != null) ctx.setVariables(model);
        return templateEngine.process(tpl.getName(), ctx);
    }
}
