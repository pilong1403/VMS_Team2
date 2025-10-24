package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "volunteerratings",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_vr_org",
                        columnNames = {"opp_id", "rater_org_id", "ratee_user_id"}
                )
        },
        indexes = {
                @Index(name = "IX_vr_opp_ratee", columnList = "opp_id, ratee_user_id"),
                @Index(name = "IX_vr_ratee_inc", columnList = "ratee_user_id")
        }
)
public class VolunteerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vr_id")
    private Integer id;

    // FK → opportunities (NOT NULL, ON DELETE CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opp_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_vr_opp"))
    private Opportunity opportunity;

    // FK → organizations (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_org_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_vr_org"))
    private Organization raterOrg;

    // FK → users (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ratee_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_vr_ratee"))
    private User rateeUser;

    // TINYINT + CHECK (1..5)
    @Min(1) @Max(5)
    @Column(name = "stars", nullable = false)
    private Byte stars;

    @Nationalized
    @Size(max = 1000)
    @Column(name = "comment", length = 1000)
    private String comment;

    // DEFAULT SYSDATETIME() (DB)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // DB không tự cập nhật -> app set
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        normalize();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        if (comment != null) comment = comment.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }

    public Organization getRaterOrg() { return raterOrg; }
    public void setRaterOrg(Organization raterOrg) { this.raterOrg = raterOrg; }

    public User getRateeUser() { return rateeUser; }
    public void setRateeUser(User rateeUser) { this.rateeUser = rateeUser; }

    public Byte getStars() { return stars; }
    public void setStars(Byte stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
