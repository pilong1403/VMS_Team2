package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "userauthproviders", schema = "dbo")
public class UserAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id") // maps PK column name
    private Integer providerId;

    // Linked user (NOT NULL). DB has ON DELETE CASCADE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Provider name, e.g., "GOOGLE" (NOT NULL)
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    // External subject/UID from the provider (NOT NULL)
    @Column(name = "external_uid", nullable = false, length = 255)
    private String externalUid;

    // Creation timestamp; populated by DB DEFAULT SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Integer getProviderId() { return providerId; }
    public void setProviderId(Integer providerId) { this.providerId = providerId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalUid() { return externalUid; }
    public void setExternalUid(String externalUid) { this.externalUid = externalUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
