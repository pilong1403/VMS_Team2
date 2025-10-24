package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // Có thể null (guest)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "FK_ticket_user"))
    private User user;

    @Nationalized
    @Email
    @Size(max = 100)
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Nationalized
    @NotBlank
    @Size(max = 255)
    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    // NVARCHAR(MAX) NOT NULL
    @Lob
    @Nationalized
    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // NOT NULL DEFAULT 'OPEN' + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TicketStatus status;

    // NOT NULL DEFAULT 'NORMAL' + CHECK
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private TicketPriority priority;

    @Nationalized
    @Size(max = 500)
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    // DB default SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // DB trigger cập nhật khi UPDATE -> read-only
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Người xử lý (có thể null); khi CLOSE bắt buộc là ADMIN (theo trigger)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by",
            foreignKey = @ForeignKey(name = "FK_ticket_resolved"))
    private User resolvedBy;

    // Child responses; DB ON DELETE CASCADE ở bảng con
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<SupportResponse> responses;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersistDefaults() {
        if (status == null)   status = TicketStatus.OPEN;
        if (priority == null) priority = TicketPriority.NORMAL;
        normalize();
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
        // Không set updatedAt ở đây; DB trigger sẽ cập nhật.
    }

    private void normalize() {
        if (contactEmail != null) contactEmail = contactEmail.trim().toLowerCase();
        if (subject != null)      subject = subject.trim();
        if (description != null)  description = description.trim();
        if (attachmentUrl != null) attachmentUrl = attachmentUrl.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }

    public List<SupportResponse> getResponses() { return responses; }
    public void setResponses(List<SupportResponse> responses) { this.responses = responses; }
}
