package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "opportunities",
        schema = "dbo",
        indexes = {
                @Index(name = "IX_opp_org", columnList = "org_id")
        }
)
public class Opportunity {

    public enum OpportunityStatus { OPEN, CLOSED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opp_id")
    private Integer oppId;

    // FK → organizations (NOT NULL). DB ON DELETE CASCADE; KHÔNG cascade REMOVE ở JPA.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "org_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_opp_org")
    )
    private Organization organization;

    // FK → categories (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_opp_cat")
    )
    private Category category;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "needed_volunteers", nullable = false)
    private Integer neededVolunteers;

    // DB: NOT NULL DEFAULT 'OPEN' + CHECK (OPEN/CLOSED/CANCELLED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OpportunityStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // DB default SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (status == null) status = OpportunityStatus.OPEN;
    }

    /**
     * Phương thức này được gọi sau khi một entity được tải từ cơ sở dữ liệu.
     * Nó sẽ tự động cập nhật trạng thái của cơ hội dựa trên thời gian kết thúc.
     */
    @PostLoad
    private void updateStatusBasedOnTime() {
        if (this.status == OpportunityStatus.OPEN && this.endTime != null && LocalDateTime.now().isAfter(this.endTime)) {
            this.status = OpportunityStatus.CLOSED;
        }
    }

    // ===== Getters & Setters =====
    public Integer getOppId() { return oppId; }
    public void setOppId(Integer oppId) { this.oppId = oppId; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getNeededVolunteers() { return neededVolunteers; }
    public void setNeededVolunteers(Integer neededVolunteers) { this.neededVolunteers = neededVolunteers; }

    public OpportunityStatus getStatus() { return status; }
    public void setStatus(OpportunityStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
