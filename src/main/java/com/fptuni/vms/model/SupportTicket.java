package com.fptuni.vms.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "supporttickets",
        schema = "dbo",
        indexes = {
                @Index(name = "IX_tickets_user", columnList = "user_id, status, priority")
        }
)
public class SupportTicket {

    public enum TicketStatus { OPEN, IN_PROGRESS, CLOSED }
    public enum TicketPriority { LOW, NORMAL, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    // May be null for guest-submitted tickets
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "FK_ticket_user")
    )
    private User user;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    // NVARCHAR(MAX) NOT NULL
    @Lob
    @Nationalized
    @Column(name = "description", nullable = false, columnDefinition="NVARCHAR(MAX)")
    private String description;

    // NOT NULL DEFAULT 'OPEN' + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TicketStatus status;

    // NOT NULL DEFAULT 'NORMAL' + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private TicketPriority priority;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    // DEFAULT SYSDATETIME() (DB)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Nullable; who resolved/owns the ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "resolved_by",
            foreignKey = @ForeignKey(name = "FK_ticket_resolved")
    )
    private User resolvedBy;

    // Child responses; DB has ON DELETE CASCADE
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<SupportResponse> responses;

    @PrePersist
    private void prePersistDefaults() {
        if (status == null)   status = TicketStatus.OPEN;
        if (priority == null) priority = TicketPriority.NORMAL;
        if (contactEmail != null) contactEmail = contactEmail.trim().toLowerCase();
    }

    // ===== Getters & Setters =====
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

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }

    public List<SupportResponse> getResponses() { return responses; }
    public void setResponses(List<SupportResponse> responses) { this.responses = responses; }
}
