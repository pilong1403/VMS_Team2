// src/main/java/com/fptuni/vms/integrations/mail/MailTemplates.java
package com.fptuni.vms.integrations.mail;

public enum MailTemplates {
    VERIFY_EMAIL("mail/otp-verify"),
    RESET_PASSWORD("mail/reset-password"),
    APPLICATION_RECEIVED("mail/application-received");

    private final String name;
    MailTemplates(String name) { this.name = name; }
    public String getName() { return name; }
}
