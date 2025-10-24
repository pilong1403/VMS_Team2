package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "supportresponses",
        schema = "dbo",
        indexes = {
                @Index(name = "IX_responses_ticket", columnList = "ticket_id, created_at")
        }
)
public class SupportResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Integer responseId;

    // FK → supporttickets (NOT NULL). DB ON DELETE CASCADE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_resp_ticket"))
    private SupportTicket ticket;

    // FK → users (NOT NULL) — trigger yêu cầu responder là ADMIN
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_resp_user"))
    private User responder;

    // NVARCHAR(MAX) NOT NULL
    @Lob
    @Nationalized
    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Nationalized
    @Size(max = 500)
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    // DEFAULT SYSDATETIME() từ DB
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        normalize();
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
    }

    private void normalize() {
        if (message != null)       message = message.trim();
        if (attachmentUrl != null) attachmentUrl = attachmentUrl.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getResponseId() { return responseId; }
    public void setResponseId(Integer responseId) { this.responseId = responseId; }

    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }

    public User getResponder() { return responder; }
    public void setResponder(User responder) { this.responder = responder; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
