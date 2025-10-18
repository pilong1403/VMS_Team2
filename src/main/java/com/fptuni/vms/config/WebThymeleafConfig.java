// src/main/java/com/fptuni/vms/config/WebThymeleafConfig.java
package com.fptuni.vms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class WebThymeleafConfig {

    @Bean
    public SpringResourceTemplateResolver webTemplateResolver() {
        SpringResourceTemplateResolver r = new SpringResourceTemplateResolver();
        r.setPrefix("classpath:/templates/");
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding("UTF-8");
        r.setCacheable(false);
        r.setCheckExistence(true);
        r.setOrder(20); // thấp hơn mail resolver
        return r;
    }

    @Bean(name = "webTemplateEngine")
    @Primary // <-- QUAN TRỌNG: để auto-config chọn engine này cho ViewResolver
    public SpringTemplateEngine webTemplateEngine(SpringResourceTemplateResolver webTemplateResolver) {
        SpringTemplateEngine te = new SpringTemplateEngine();
        te.addTemplateResolver(webTemplateResolver);
        return te;
    }
}
