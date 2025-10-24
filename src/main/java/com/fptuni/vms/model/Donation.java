package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations", schema = "dbo")
public class Donation {

    public enum DonationStatus { PENDING, PAID, FAILED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Integer donationId;

    // donor_id: NULLable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", foreignKey = @ForeignKey(name = "FK_dn_donor"))
    private User donor;

    // opp_id: NULLable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", foreignKey = @ForeignKey(name = "FK_dn_opp"))
    private Opportunity opportunity;

    @NotNull
    @DecimalMin(value = "0.01") // khớp CHECK (amount > 0)
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Nationalized
    @Column(name = "method", length = 50)
    private String method;

    // DB DEFAULT 'PENDING' + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DonationStatus status;

    @Nationalized
    @Column(name = "content", length = 255)
    private String content;

    @Nationalized
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    // DB DEFAULT SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------- Provider / Payment tracking ----------
    @Nationalized
    @Column(name = "provider", length = 30)
    private String provider; // ví dụ: "VNPAY", "MOMO"

    @Nationalized
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    // DB tự gán khi status = PAID
    @Column(name = "paid_at", insertable = false, updatable = false)
    private LocalDateTime paidAt;

    // -------- Refund tracking ----------
    // DB tự gán khi status = REFUNDED
    @Column(name = "refunded_at", insertable = false, updatable = false)
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 18, scale = 2)
    private BigDecimal refundAmount; // DB CHECK (>= 0)

    @Nationalized
    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_by", foreignKey = @ForeignKey(name = "FK_dn_refund"))
    private User refundBy; // trigger yêu cầu phải là ADMIN khi REFUNDED

    @Nationalized
    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Lob
    @Nationalized
    @Column(name = "provider_payload", columnDefinition = "NVARCHAR(MAX)")
    private String providerPayload;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        if (status == null) status = DonationStatus.PENDING; // mirror DB DEFAULT
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now(); // DB không auto-update cột này
    }

    /* ======================
       GETTERS & SETTERS
       ====================== */
    public Integer getDonationId() { return donationId; }
    public void setDonationId(Integer donationId) { this.donationId = donationId; }

    public User getDonor() { return donor; }
    public void setDonor(User donor) { this.donor = donor; }

    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public DonationStatus getStatus() { return status; }
    public void setStatus(DonationStatus status) { this.status = status; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getPaidAt() { return paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public User getRefundBy() { return refundBy; }
    public void setRefundBy(User refundBy) { this.refundBy = refundBy; }

    public String getProviderRefundId() { return providerRefundId; }
    public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }

    public String getProviderPayload() { return providerPayload; }
    public void setProviderPayload(String providerPayload) { this.providerPayload = providerPayload; }
}
