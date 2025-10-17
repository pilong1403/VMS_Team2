// src/main/java/com/fptuni/vms/service/impl/OtpVerificationServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.integrations.mail.MailComposer;
import com.fptuni.vms.integrations.mail.MailTemplates;
import com.fptuni.vms.model.OtpVerification;
import com.fptuni.vms.model.OtpVerification.Purpose;
import com.fptuni.vms.repository.OtpVerificationRepository;
import com.fptuni.vms.service.OtpVerificationService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpVerificationServiceImpl implements OtpVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 10;

    private final OtpVerificationRepository repo;
    private final MailComposer mailComposer;
    private final SecureRandom random = new SecureRandom();

    public OtpVerificationServiceImpl(OtpVerificationRepository repo, MailComposer mailComposer) {
        this.repo = repo;
        this.mailComposer = mailComposer;
    }

    @Override
    public void generateAndSendOtp(String email, String purpose) {
        final Purpose p = toPurpose(purpose);

        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(EXPIRE_MINUTES);

        OtpVerification v = new OtpVerification();
        v.setEmail(email);
        v.setOtpCode(code);
        v.setExpiredAt(expiredAt);
        v.setVerified(false);
        v.setPurpose(p);
        repo.save(v);

        Map<String, Object> model = new HashMap<>();
        model.put("fullName", null); // nếu có tên user, set tại đây
        model.put("code", code);
        model.put("minutes", EXPIRE_MINUTES);

        String subject = "Your VMS verification code";
        mailComposer.sendTemplateHtml(email, subject, MailTemplates.VERIFY_EMAIL, model);
    }

    @Override
    public void verifyOtp(String email, String purpose, String code) throws OtpException {
        final Purpose p = toPurpose(purpose);

        OtpVerification v = repo
                .findTop1ByEmailAndPurposeOrderByCreatedAtDesc(email, p)
                .orElseThrow(() -> new OtpException("OTP_NOT_FOUND"));

        if (Boolean.TRUE.equals(v.getVerified())) {
            throw new OtpException("OTP_ALREADY_USED");
        }

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (v.getExpiredAt() != null && v.getExpiredAt().isBefore(nowUtc)) {
            throw new OtpException("OTP_EXPIRED");
        }

        if (!v.getOtpCode().equals(code)) {
            throw new OtpException("OTP_INVALID");
        }

        v.setVerified(true);
        v.setConsumedAt(nowUtc);
        repo.save(v);
    }

    private Purpose toPurpose(String purpose) {
        try {
            if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException();
            return Purpose.valueOf(purpose);
        } catch (IllegalArgumentException ex) {
            throw new OtpException("OTP_PURPOSE_INVALID");
        }
    }
}
