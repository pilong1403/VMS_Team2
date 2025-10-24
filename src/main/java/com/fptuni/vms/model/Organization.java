package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "organizations",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_org_owner", columnNames = "owner_id")
        }
)
public class Organization {

    public enum RegStatus { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Integer orgId;

    // Owner user (NOT NULL). DB ON DELETE NO ACTION.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_org_owner"))
    private User owner;

    @Nationalized
    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // NVARCHAR(MAX)
    @Lob
    @Nationalized
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // DB default SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // Trigger trg_org_update_sync tự cập nhật khi UPDATE
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // --- Registration fields ---
    @Enumerated(EnumType.STRING)
    @Column(name = "reg_status", nullable = false, length = 20)
    private RegStatus regStatus; // PENDING/APPROVED/REJECTED

    @Nationalized
    @Size(max = 500)
    @Column(name = "reg_doc_url", length = 500)
    private String regDocUrl;

    @Nationalized
    @Size(max = 200)
    @Column(name = "reg_doc_cloud_id", length = 200)
    private String regDocCloudId;

    @Nationalized
    @Size(max = 500)
    @Column(name = "reg_note", length = 500)
    private String regNote;

    @Column(name = "reg_submitted_at")
    private LocalDateTime regSubmittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reg_reviewed_by",
            foreignKey = @ForeignKey(name = "FK_org_reviewed_by"))
    private User regReviewedBy;

    @Column(name = "reg_reviewed_at")
    private LocalDateTime regReviewedAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        if (regStatus == null) regStatus = RegStatus.PENDING; // mirror DB default
        normalize();
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
        // updated_at do DB trigger set; không tự set ở đây
    }

    private void normalize() {
        if (name != null)         name = name.trim();
        if (description != null)  description = description.trim();
        if (regDocUrl != null)    regDocUrl = regDocUrl.trim();
        if (regDocCloudId != null) regDocCloudId = regDocCloudId.trim();
        if (regNote != null)      regNote = regNote.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getOrgId() { return orgId; }
    public void setOrgId(Integer orgId) { this.orgId = orgId; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public RegStatus getRegStatus() { return regStatus; }
    public void setRegStatus(RegStatus regStatus) { this.regStatus = regStatus; }

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
