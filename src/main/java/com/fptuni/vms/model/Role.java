package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "roles",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_roles_role_name", columnNames = "role_name")
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Nationalized
    @NotBlank
    @Size(max = 50)
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Nationalized
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    // DB: DEFAULT SYSDATETIME()
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // DB không auto-update -> app set
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ======================
       Lifecycle Hooks
       ====================== */
    @PrePersist
    private void prePersist() {
        normalize();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        normalize();
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        if (roleName != null) roleName = roleName.trim().toUpperCase();
        if (description != null) description = description.trim();
    }

    /* ======================
       Getters & Setters
       ====================== */
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
