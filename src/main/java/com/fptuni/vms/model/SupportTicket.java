package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "supporttickets", schema = "dbo")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    // May be null for guest-submitted tickets
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    // Long text (NVARCHAR(MAX) in DB). NOT NULL.
    @Column(name = "description", nullable = false)
    private String description;

    // NOT NULL, default 'OPEN' at DB
    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN"; // OPEN / IN_PROGRESS / CLOSED

    // NOT NULL, default 'NORMAL' at DB
    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "NORMAL"; // LOW / NORMAL / HIGH

    // DB default SYSDATETIME(); let DB populate it
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Nullable; who resolved/owns the ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    // Child responses; DB has ON DELETE CASCADE (safe to mirror with REMOVE)
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<SupportResponse> responses;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }

    public List<SupportResponse> getResponses() { return responses; }
    public void setResponses(List<SupportResponse> responses) { this.responses = responses; }
}
