package com.fptuni.vms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Set;

@Configuration
public class MailConfig {

    @Bean(name = "mailTemplateResolver")
    public ClassLoaderTemplateResolver mailTemplateResolver() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        // Sẽ tìm template tại: src/main/resources/templates/mail/...
        r.setPrefix("templates/");
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding("UTF-8");
        r.setCacheable(false);
        r.setCheckExistence(true);
        r.setResolvablePatterns(Set.of("mail/*")); // chỉ bắt mail/*
        r.setOrder(5); // ưu tiên cao hơn resolver web
        return r;
    }

    @Bean(name = "mailTemplateEngine")
    public SpringTemplateEngine mailTemplateEngine(ClassLoaderTemplateResolver mailTemplateResolver) {
        SpringTemplateEngine te = new SpringTemplateEngine();
        te.addTemplateResolver(mailTemplateResolver);
        return te;
    }
}
