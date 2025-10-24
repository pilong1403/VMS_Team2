package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "userauthproviders",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_uap_provider_uid", columnNames = {"provider", "external_uid"})
        }
)
public class UserAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Integer providerId;

    // FK → users (NOT NULL). DB ON DELETE CASCADE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_uap_user"))
    private User user;

    @Nationalized
    @NotBlank
    @Size(max = 20)
    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // ví dụ: GOOGLE

    @Nationalized
    @NotBlank
    @Size(max = 255)
    @Column(name = "external_uid", nullable = false, length = 255)
    private String externalUid;

    // DEFAULT SYSDATETIME() (DB)
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist @PreUpdate
    private void normalize() {
        if (provider != null)    provider = provider.trim().toUpperCase();
        if (externalUid != null) externalUid = externalUid.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getProviderId() { return providerId; }
    public void setProviderId(Integer providerId) { this.providerId = providerId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalUid() { return externalUid; }
    public void setExternalUid(String externalUid) { this.externalUid = externalUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
