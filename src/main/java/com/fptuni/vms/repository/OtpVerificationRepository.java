// src/main/java/com/fptuni/vms/repository/OtpVerificationRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.OtpVerification;

import java.util.Optional;

public interface OtpVerificationRepository {

    Optional<OtpVerification> findTop1ByEmailAndPurposeOrderByCreatedAtDesc(
            String email, OtpVerification.Purpose purpose);

    /** Insert if otpId == null, else update. */
    OtpVerification save(OtpVerification v);

    /** Đếm OTP đang hoạt động (verified = 0 và chưa hết hạn). */
    int countActiveByEmailAndPurpose(String email, OtpVerification.Purpose purpose);

    /** Vô hiệu toàn bộ OTP đang hoạt động (đánh dấu verified=1 + consumed_at). */
    int invalidateActiveByEmailAndPurpose(String email, OtpVerification.Purpose purpose);
}
