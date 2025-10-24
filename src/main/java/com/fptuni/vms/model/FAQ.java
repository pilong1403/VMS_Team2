package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq", schema = "dbo")
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Integer faqId;

    @Nationalized
    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @Nationalized
    @NotBlank
    @Size(max = 255)
    @Column(name = "question", length = 255, nullable = false)
    private String question;

    // NVARCHAR(MAX)
    @Lob
    @Nationalized
    @Column(name = "answer", columnDefinition = "NVARCHAR(MAX)")
    private String answer;

    // BIT NOT NULL DEFAULT 1
    @Column(name = "status", nullable = false)
    private Boolean status; // true = visible, false = hidden

    // DEFAULT SYSDATETIME(), NOT NULL (DB side)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // DB không tự cập nhật -> app set bằng @PreUpdate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "FK_faq_updated_by")
    )
    private User updatedBy;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        if (status == null) status = Boolean.TRUE; // mirror DB default (1)
        normalize();
        if (updatedAt == null) updatedAt = LocalDateTime.now(); // lần đầu
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        if (category != null)  category  = category.trim();
        if (question != null)  question  = question.trim();
        if (answer != null)    answer    = answer.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getFaqId() { return faqId; }
    public void setFaqId(Integer faqId) { this.faqId = faqId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
}
