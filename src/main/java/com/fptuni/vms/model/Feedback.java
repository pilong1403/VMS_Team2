package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback", schema = "dbo")
public class Feedback {

    public enum FeedbackType { VOLUNTEER, ORG }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "opp_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_fb_opp")
    )
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_fb_user")
    )
    private User user;

    // NVARCHAR(MAX)
    @Lob
    @Nationalized
    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    // NULLable theo DDL; nếu có giá trị thì 1..5
    @Min(1) @Max(5)
    @Column(name = "rating")
    private Integer rating;

    // NVARCHAR(20) NOT NULL DEFAULT 'VOLUNTEER'
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 20, nullable = false)
    private FeedbackType feedbackType;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    // DEFAULT SYSDATETIME() từ DB
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        if (feedbackType == null) feedbackType = FeedbackType.VOLUNTEER;
    }

    // ====================== GETTERS & SETTERS ======================

    public Integer getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Integer feedbackId) { this.feedbackId = feedbackId; }

    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
