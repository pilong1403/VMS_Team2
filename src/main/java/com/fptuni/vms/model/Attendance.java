package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", schema = "dbo")
public class Attendance {

    public enum AttendanceStatus {
        PRESENT, ABSENT, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "att_id")
    private Integer attId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "app_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_att_app")
    )
    private Application application;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;

    // DB: NOT NULL DEFAULT 'PRESENT' + CHECK (PRESENT/ABSENT/COMPLETED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AttendanceStatus status;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "proof_file_url", length = 500)
    private String proofFileUrl;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = AttendanceStatus.PRESENT; // mirror DEFAULT DB
        }
    }

    // ===== Getters & Setters =====
    public Integer getAttId() { return attId; }
    public void setAttId(Integer attId) { this.attId = attId; }

    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }

    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }

    public LocalDateTime getCheckoutTime() { return checkoutTime; }
    public void setCheckoutTime(LocalDateTime checkoutTime) { this.checkoutTime = checkoutTime; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getProofFileUrl() { return proofFileUrl; }
    public void setProofFileUrl(String proofFileUrl) { this.proofFileUrl = proofFileUrl; }

    // Helper tiện dùng trong service
    @Transient
    public boolean isOpenSession() {
        return checkinTime != null && checkoutTime == null;
    }
}
