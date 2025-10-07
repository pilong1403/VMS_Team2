package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities", schema = "dbo")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opp_id")
    private Integer oppId;

    // FK → organizations (NOT NULL). DB has ON DELETE CASCADE; do NOT cascade REMOVE in JPA.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    // FK → categories (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "needed_volunteers", nullable = false)
    private Integer neededVolunteers;

    // NOT NULL, default 'OPEN' at DB
    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN"; // OPEN / CLOSED / CANCELLED

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // DB default SYSDATETIME(); let DB populate it
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

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

    public Integer getNeededVolunteers() { return neededVolunteers; }
    public void setNeededVolunteers(Integer neededVolunteers) { this.neededVolunteers = neededVolunteers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
