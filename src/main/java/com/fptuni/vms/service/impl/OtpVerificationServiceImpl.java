package com.fptuni.vms.service.impl;

import com.fptuni.vms.integrations.mail.MailService;
import com.fptuni.vms.integrations.mail.MailTemplates;
import com.fptuni.vms.model.OtpVerification;
import com.fptuni.vms.model.OtpVerification.Purpose;
import com.fptuni.vms.repository.OtpVerificationRepository;
import com.fptuni.vms.service.OtpVerificationService;
import jakarta.transaction.Transactional;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Transactional
public class OtpVerificationServiceImpl implements OtpVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 10;

    private final OtpVerificationRepository repo;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public OtpVerificationServiceImpl(OtpVerificationRepository repo, MailService mailService) {
        this.repo = repo;
        this.mailService = mailService;
    }

    @Override
    public void generateAndSendOtp(String email, String purpose) {
        final Purpose p = toPurpose(purpose);

        // A) Nếu đã có OTP còn hiệu lực -> ném lỗi rõ ràng
        int active = repo.countActiveByEmailAndPurpose(email, p);
        if (active > 0) throw new ActiveOtpExistsException();

        // Sinh mã OTP
        String code = String.format("%0" + OTP_LENGTH + "d", random.nextInt((int) Math.pow(10, OTP_LENGTH)));
        LocalDateTime expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(EXPIRE_MINUTES);

        OtpVerification v = new OtpVerification();
        v.setEmail(email);
        v.setOtpCode(code);
        v.setExpiredAt(expiredAt);
        v.setVerified(false);
        v.setPurpose(p);
        repo.save(v);

        // Chuẩn bị context cho mail template
        Context ctx = new Context();
        ctx.setVariable("fullName", null);
        ctx.setVariable("code", code);
        ctx.setVariable("minutes", EXPIRE_MINUTES);

        String subject = "Your VMS verification code";
        try {
            mailService.sendTemplate(email, subject, MailTemplates.VERIFY_EMAIL.getName(), ctx);
        } catch (MailException e) {
            // Nếu gửi thất bại, vô hiệu OTP vừa tạo
            repo.invalidateActiveByEmailAndPurpose(email, p);
            throw new MailSendException(e);
        }
    }

    @Override
    public void verifyOtp(String email, String purpose, String code) throws OtpException {
        final Purpose p = toPurpose(purpose);

        OtpVerification v = repo
                .findTop1ByEmailAndPurposeOrderByCreatedAtDesc(email, p)
                .orElseThrow(() -> new OtpException("OTP_NOT_FOUND"));

        if (Boolean.TRUE.equals(v.getVerified()))
            throw new OtpException("OTP_ALREADY_USED");

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (v.getExpiredAt() != null && v.getExpiredAt().isBefore(nowUtc))
            throw new OtpException("OTP_EXPIRED");

        if (!v.getOtpCode().equals(code))
            throw new OtpException("OTP_INVALID");

        v.setVerified(true);
        v.setConsumedAt(nowUtc);
        repo.save(v);
    }

    // src/main/java/com/fptuni/vms/service/impl/OtpVerificationServiceImpl.java
    private Purpose toPurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new OtpException("OTP_PURPOSE_INVALID");
        }
        String key = purpose.trim().toUpperCase();

        if ("ORG_REGISTER_VERIFY".equals(key)) {
            key = "ORG_REGISTER";
        }

        try {
            return Purpose.valueOf(key);   // tên phải trùng với enum Purpose
        } catch (IllegalArgumentException ex) {
            throw new OtpException("OTP_PURPOSE_INVALID");
        }
    }

}
