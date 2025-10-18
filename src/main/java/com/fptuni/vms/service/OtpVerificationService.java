// src/main/java/com/fptuni/vms/service/OtpVerificationService.java
package com.fptuni.vms.service;

public interface OtpVerificationService {

    // Unchecked exception -> không bắt buộc try/catch ở controller
    class OtpException extends RuntimeException {
        public OtpException(String msg) { super(msg); }
        public OtpException(String msg, Throwable cause) { super(msg, cause); }
    }
    /** Đã tồn tại OTP còn hiệu lực. */
    class ActiveOtpExistsException extends OtpException {
        public ActiveOtpExistsException() { super("OTP_ACTIVE_EXISTS"); }
    }
    /** Lỗi gửi mail. */
    class MailSendException extends OtpException {
        public MailSendException(Throwable cause) { super("MAIL_SEND_FAILED", cause); }
    }

    void generateAndSendOtp(String email, String purpose);
    void verifyOtp(String email, String purpose, String code) throws OtpException;
}
