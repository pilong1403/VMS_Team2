package com.fptuni.vms.model;

import jakarta.persistence.*;

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

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    @PrePersist @PreUpdate
    private void normalize() {
        if (roleName != null) roleName = roleName.trim().toUpperCase(); // hoặc giữ nguyên tuỳ convention
        if (description != null) description = description.trim();
    }

    // Getters & Setters
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
