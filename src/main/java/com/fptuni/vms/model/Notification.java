package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        schema = "dbo",
        indexes = {
                // DB tạo index có created_at DESC; JPA không hỗ trợ DESC -> để mặc định.
                @Index(name = "IX_notifications_inbox", columnList = "user_id, is_read, created_at")
        }
)
public class Notification {

    public enum NotificationType { INFO, ALERT, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_id")
    private Integer notiId;

    // Recipient (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_noti_user"))
    private User user;

    // NVARCHAR(MAX) NOT NULL
    @Lob
    @Nationalized
    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    // CHECK (INFO/ALERT/SYSTEM), NULLABLE
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private NotificationType type;

    // BIT NOT NULL DEFAULT 0
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = Boolean.FALSE;

    // DEFAULT SYSDATETIME() (DB side)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Nationalized
    @Size(max = 200)
    @Column(name = "title", length = 200)
    private String title;

    @Nationalized
    @Size(max = 500)
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @ForeignKey(name = "FK_noti_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id",
            foreignKey = @ForeignKey(name = "FK_noti_org"))
    private Organization organization;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        normalize();
        if (isRead == null) isRead = Boolean.FALSE; // mirror DB default
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
    }

    private void normalize() {
        if (message != null)  message  = message.trim();
        if (title != null)    title    = title.trim();
        if (linkUrl != null)  linkUrl  = linkUrl.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getNotiId() { return notiId; }
    public void setNotiId(Integer notiId) { this.notiId = notiId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
}
