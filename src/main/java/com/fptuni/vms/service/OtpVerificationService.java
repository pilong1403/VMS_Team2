// src/main/java/com/fptuni/vms/service/OtpVerificationService.java
package com.fptuni.vms.service;

public interface OtpVerificationService {

    // Unchecked exception -> không bắt buộc try/catch ở controller
    class OtpException extends RuntimeException {
        public OtpException(String msg) { super(msg); }
    }

    void generateAndSendOtp(String email, String purpose);

    void verifyOtp(String email, String purpose, String code) throws OtpException;
}
