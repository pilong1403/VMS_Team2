package com.fptuni.vms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "cloudinary")
@Validated
public class CloudinaryProps {
    @NotBlank private String cloudName;
    @NotBlank private String apiKey;
    @NotBlank private String apiSecret;

    public String getCloudName() { return cloudName; }
    public void setCloudName(String cloudName) { this.cloudName = cloudName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
}
