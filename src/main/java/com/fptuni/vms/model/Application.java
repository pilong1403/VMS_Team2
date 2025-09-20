package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_id")
    private Integer appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opp_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private User volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ======================
    // GETTER & SETTER
    // ======================

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public User getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(User volunteer) {
        this.volunteer = volunteer;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
