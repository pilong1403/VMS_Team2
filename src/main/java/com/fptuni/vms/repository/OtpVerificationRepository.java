package com.fptuni.vms.repository;

import com.fptuni.vms.model.OtpVerification;

import java.util.Optional;

public interface OtpVerificationRepository {

    Optional<OtpVerification> findTop1ByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpVerification.Purpose purpose);

    /**
     * Insert if otpId == null, else update.
     */
    OtpVerification save(OtpVerification v);
}
