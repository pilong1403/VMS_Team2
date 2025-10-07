package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunitysections", schema = "dbo")
public class OpportunitySection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Integer sectionId;

    // FK → opportunities (NOT NULL). DB has ON DELETE CASCADE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "heading", length = 255)
    private String heading;

    // Long text (NVARCHAR(MAX) in DB)
    @Column(name = "content")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "caption", length = 255)
    private String caption;

    // DB default SYSDATETIME(); let DB populate it
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }

    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }

    public Integer getSectionOrder() { return sectionOrder; }
    public void setSectionOrder(Integer sectionOrder) { this.sectionOrder = sectionOrder; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
