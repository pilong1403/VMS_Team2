package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizations", schema = "dbo")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Integer orgId;

    // Owner user (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // Long text (NVARCHAR(MAX) in DB)
    @Column(name = "description")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // NOT NULL DEFAULT 0
    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    // DB default SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ---------- Registration fields merged into organizations ----------
    // NOT NULL DEFAULT 'PENDING' (values: PENDING, APPROVED, REJECTED)
    @Column(name = "reg_status", nullable = false, length = 20)
    private String regStatus = "PENDING";

    @Column(name = "reg_doc_url", length = 500)
    private String regDocUrl;

    @Column(name = "reg_doc_cloud_id", length = 200)
    private String regDocCloudId;

    @Column(name = "reg_note", length = 500)
    private String regNote;

    @Column(name = "reg_submitted_at")
    private LocalDateTime regSubmittedAt;

    // Reviewer (users.user_id), nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reg_reviewed_by")
    private User regReviewedBy;

    @Column(name = "reg_reviewed_at")
    private LocalDateTime regReviewedAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getOrgId() { return orgId; }
    public void setOrgId(Integer orgId) { this.orgId = orgId; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRegStatus() { return regStatus; }
    public void setRegStatus(String regStatus) { this.regStatus = regStatus; }

    public String getRegDocUrl() { return regDocUrl; }
    public void setRegDocUrl(String regDocUrl) { this.regDocUrl = regDocUrl; }

    public String getRegDocCloudId() { return regDocCloudId; }
    public void setRegDocCloudId(String regDocCloudId) { this.regDocCloudId = regDocCloudId; }

    public String getRegNote() { return regNote; }
    public void setRegNote(String regNote) { this.regNote = regNote; }

    public LocalDateTime getRegSubmittedAt() { return regSubmittedAt; }
    public void setRegSubmittedAt(LocalDateTime regSubmittedAt) { this.regSubmittedAt = regSubmittedAt; }

    public User getRegReviewedBy() { return regReviewedBy; }
    public void setRegReviewedBy(User regReviewedBy) { this.regReviewedBy = regReviewedBy; }

    public LocalDateTime getRegReviewedAt() { return regReviewedAt; }
    public void setRegReviewedAt(LocalDateTime regReviewedAt) { this.regReviewedAt = regReviewedAt; }
}
