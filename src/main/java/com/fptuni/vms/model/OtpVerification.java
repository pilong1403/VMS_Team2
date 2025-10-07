package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otpverification", schema = "dbo")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Integer otpId;

    // Email to verify / reset (NOT NULL)
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    // OTP code content (NOT NULL)
    @Column(name = "otp_code", length = 10, nullable = false)
    private String otpCode;

    // Expiration timestamp (NOT NULL)
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    // NOT NULL DEFAULT 0
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    // DB default SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // Optional purpose tag: RESET_PASSWORD / VERIFY_EMAIL / ...
    @Column(name = "purpose", length = 30)
    private String purpose;

    // Optional token (e.g., link-based verification)
    @Column(name = "token", length = 200)
    private String token;

    // When the OTP/token was consumed (nullable)
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getOtpId() { return otpId; }
    public void setOtpId(Integer otpId) { this.otpId = otpId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
