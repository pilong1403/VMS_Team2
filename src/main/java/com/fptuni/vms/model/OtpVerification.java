package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "otpverification", schema = "dbo")
public class OtpVerification {

    // Phải khớp DDL: VERIFY_EMAIL / RESET_PASSWORD / ORG_REGISTER
    public enum Purpose {
        VERIFY_EMAIL,
        RESET_PASSWORD,
        ORG_REGISTER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Integer otpId;

    @Nationalized
    @Email
    @NotBlank
    @Size(max = 100)
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Nationalized
    @NotBlank
    @Size(max = 10)
    @Column(name = "otp_code", length = 10, nullable = false)
    private String otpCode;

    @NotNull
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    // BIT NOT NULL DEFAULT 0 (DB)
    @Column(name = "verified", nullable = false)
    private Boolean verified = Boolean.FALSE;

    // DEFAULT SYSDATETIME() (DB side)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // NULLABLE + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30)
    private Purpose purpose;

    @Nationalized
    @Size(max = 200)
    @Column(name = "token", length = 200)
    private String token;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist @PreUpdate
    private void normalize() {
        if (email != null) email = email.trim().toLowerCase();
        if (otpCode != null) otpCode = otpCode.trim();
        if (token != null) token = token.trim();
        if (verified == null) verified = Boolean.FALSE; // mirror DB default
    }

    /* ======================
       Getters & Setters
       ====================== */
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

    public Purpose getPurpose() { return purpose; }
    public void setPurpose(Purpose purpose) { this.purpose = purpose; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
