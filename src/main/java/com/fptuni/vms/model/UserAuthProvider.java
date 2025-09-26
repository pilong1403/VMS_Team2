package com.fptuni.vms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "userauthproviders", schema = "dbo")
public class UserAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")                 // <-- map đúng tên cột
    private Integer providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "external_uid", nullable = false, length = 255)
    private String externalUid;

    @Column(name = "created_at", insertable = false, updatable = false)  // DB tự set DEFAULT SYSDATETIME()
    private LocalDateTime createdAt;

    // GETTER & SETTER
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
