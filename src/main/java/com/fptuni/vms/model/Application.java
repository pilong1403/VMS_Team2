package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications", schema = "dbo", indexes = {
                @Index(name = "IX_app_opp_status", columnList = "opp_id, status")
})
public class Application {

        public enum ApplicationStatus {
                PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
        }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "app_id")
        private Integer appId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "opp_id", nullable = false, foreignKey = @ForeignKey(name = "FK_app_opp"))
        private Opportunity opportunity;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "volunteer_id", nullable = false, foreignKey = @ForeignKey(name = "FK_app_volunteer"))
        private User volunteer;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "processed_by", foreignKey = @ForeignKey(name = "FK_app_processed"))
        private User processedBy;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", length = 20, nullable = false)
        private ApplicationStatus status; // DB CHECK: PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED

        @Column(name = "reason", length = 255)
        private String reason;

        @Column(name = "cancel_reason", length = 255)
        private String cancelReason;

        // DB default SYSDATETIME(); NOT NULL
        @Column(name = "applied_at", nullable = false)
        private LocalDateTime appliedAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        private void prePersist() {
                if (status == null) {
                        status = ApplicationStatus.PENDING; // mirror DB DEFAULT
                }
        }

        // ======================
        // GETTERS & SETTERS
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

        public ApplicationStatus getStatus() {
                return status;
        }

        public void setStatus(ApplicationStatus status) {
                this.status = status;
        }

        public String getReason() {
                return reason;
        }

        public void setReason(String reason) {
                this.reason = reason;
        }

        public String getCancelReason() {
                return cancelReason;
        }

        public void setCancelReason(String cancelReason) {
                this.cancelReason = cancelReason;
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
