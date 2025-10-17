// src/main/java/com/fptuni/vms/config/MailConfig.java
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
        // Bạn đang gọi templateName = "mail/otp-verify"
        r.setPrefix("templates/");    // => sẽ tìm templates/mail/otp-verify.html
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding("UTF-8");
        r.setCacheable(false);
        r.setCheckExistence(true);
        r.setResolvablePatterns(Set.of("mail/*")); // CHỈ bắt mail/*
        r.setOrder(5); // ưu tiên cao hơn web
        return r;
    }

    @Bean(name = "mailTemplateEngine")
    public SpringTemplateEngine mailTemplateEngine(ClassLoaderTemplateResolver mailTemplateResolver) {
        SpringTemplateEngine te = new SpringTemplateEngine();
        te.addTemplateResolver(mailTemplateResolver);
        return te;
    }
}
