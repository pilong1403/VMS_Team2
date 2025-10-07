package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "volunteerratings",
        schema = "dbo",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_vr_org",
                columnNames = {"opp_id", "rater_org_id", "ratee_user_id"}
        )
)
public class VolunteerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vr_id")
    private Integer id;

    // Opportunity being rated (NOT NULL). DB: FK with ON DELETE CASCADE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    // Organization that gives the rating (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_org_id", nullable = false)
    private Organization raterOrg;

    // Volunteer (user) who is rated (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ratee_user_id", nullable = false)
    private User rateeUser;

    // 1..5 (DB TINYINT with CHECK). Validate in service/Bean Validation if desired.
    @Column(name = "stars", nullable = false)
    private Short stars;

    @Column(name = "comment", length = 1000)
    private String comment;

    // DB default SYSDATETIME(); let DB populate it
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }

    public Organization getRaterOrg() { return raterOrg; }
    public void setRaterOrg(Organization raterOrg) { this.raterOrg = raterOrg; }

    public User getRateeUser() { return rateeUser; }
    public void setRateeUser(User rateeUser) { this.rateeUser = rateeUser; }

    public Short getStars() { return stars; }
    public void setStars(Short stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
