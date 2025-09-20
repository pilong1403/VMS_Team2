package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Supportresponses")
public class SupportResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Integer responseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    @Column(name = "message", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // GETTER & SETTER
    public Integer getResponseId() {
        return responseId;
    }

    public void setResponseId(Integer responseId) {
        this.responseId = responseId;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public User getResponder() {
        return responder;
    }

    public void setResponder(User responder) {
        this.responder = responder;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
