package com.fptuni.vms.model;

import jakarta.persistence.*;
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

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount; // DB CHECK (amount > 0)

    @Column(name = "method", length = 50)
    private String method;

    // DB: NOT NULL DEFAULT 'PENDING' + CHECK (PENDING/PAID/FAILED/REFUNDED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DonationStatus status;

    @Column(name = "content", length = 255)
    private String content;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    // DEFAULT SYSDATETIME() (DB side)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------- Provider / Payment tracking ----------
    @Column(name = "provider", length = 30)
    private String provider; // e.g., "VNPAY", "MOMO"... (tùy bạn chuẩn hoá bằng enum nếu muốn)

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // -------- Refund tracking ----------
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 18, scale = 2)
    private BigDecimal refundAmount; // DB CHECK (>= 0)

    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_by", foreignKey = @ForeignKey(name = "FK_dn_refund"))
    private User refundBy;

    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Lob
    @Nationalized
    @Column(name = "provider_payload", columnDefinition = "NVARCHAR(MAX)")
    private String providerPayload;

    // ---------- Lifecycle: mặc định status như DB ----------
    @PrePersist
    private void prePersist() {
        if (status == null) status = DonationStatus.PENDING;
    }

    // ====================== GETTERS & SETTERS ======================

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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

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
