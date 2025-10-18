// src/main/java/com/fptuni/vms/service/impl/OtpVerificationServiceImpl.java
package com.fptuni.vms.service.impl;

import com.fptuni.vms.integrations.mail.MailComposer;
import com.fptuni.vms.integrations.mail.MailTemplates;
import com.fptuni.vms.model.OtpVerification;
import com.fptuni.vms.model.OtpVerification.Purpose;
import com.fptuni.vms.repository.OtpVerificationRepository;
import com.fptuni.vms.service.OtpVerificationService;
import jakarta.transaction.Transactional;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
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

        // A) Nếu đã có OTP còn hiệu lực -> ném lỗi rõ ràng
        int active = repo.countActiveByEmailAndPurpose(email, p);
        if (active > 0) throw new ActiveOtpExistsException();

        // B) (Tuỳ chọn) Tự vô hiệu OTP cũ rồi tiếp tục
        // repo.invalidateActiveByEmailAndPurpose(email, p);

        // Sinh OTP
        String code = String.format("%0" + OTP_LENGTH + "d", random.nextInt((int) Math.pow(10, OTP_LENGTH)));
        LocalDateTime expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(EXPIRE_MINUTES);

        OtpVerification v = new OtpVerification();
        v.setEmail(email);
        v.setOtpCode(code);
        v.setExpiredAt(expiredAt);
        v.setVerified(false);
        v.setPurpose(p);
        repo.save(v);

        // Gửi mail
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", null);
        model.put("code", code);
        model.put("minutes", EXPIRE_MINUTES);

        String subject = "Your VMS verification code";
        try {
            mailComposer.sendTemplateHtml(email, subject, MailTemplates.VERIFY_EMAIL, model);
        } catch (MailException e) { // <-- chỉ bắt MailException
            // Nếu gửi thất bại, vô hiệu OTP vừa tạo (tránh “OTP tồn tại nhưng user không nhận được”)
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

        if (Boolean.TRUE.equals(v.getVerified())) throw new OtpException("OTP_ALREADY_USED");

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (v.getExpiredAt() != null && v.getExpiredAt().isBefore(nowUtc)) throw new OtpException("OTP_EXPIRED");

        if (!v.getOtpCode().equals(code)) throw new OtpException("OTP_INVALID");

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
