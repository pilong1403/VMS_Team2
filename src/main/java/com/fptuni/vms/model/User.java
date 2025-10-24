package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_users_email", columnNames = "email")
        }
)
public class User {

        public enum UserStatus { ACTIVE, LOCKED }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "user_id")
        private Integer userId;

        @Nationalized
        @NotBlank
        @Size(max = 100)
        @Column(name = "full_name", nullable = false, length = 100)
        private String fullName;

        @Nationalized
        @NotBlank
        @Email
        @Size(max = 100)
        @Column(name = "email", nullable = false, length = 100)
        private String email;

        @Nationalized
        @Size(max = 20)
        @Column(name = "phone", length = 20)
        private String phone;

        @Nationalized
        @Size(max = 500)
        @Column(name = "avatar_url", length = 500)
        private String avatarUrl;

        @NotBlank
        @Size(max = 255)
        @Column(name = "password_hash", nullable = false, length = 255)
        private String passwordHash;

        // FK → roles (NOT NULL)
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "role_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_users_role"))
        private Role role;

        // NOT NULL DEFAULT 'ACTIVE' + CHECK
        @Enumerated(EnumType.STRING)
        @Column(name = "status", length = 20, nullable = false)
        private UserStatus status;

        @Nationalized
        @Size(max = 500)
        @Column(name = "address", length = 500)
        private String address;

        // DEFAULT SYSDATETIME() (DB)
        @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
        private LocalDateTime createdAt;

        // DB không auto-update → app set; cũng có thể được trigger org cập nhật hộ
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        /* ======================
           Lifecycle Hooks
           ====================== */
        @PrePersist
        private void prePersist() {
                normalize();
                if (status == null) status = UserStatus.ACTIVE; // mirror DB default
                if (updatedAt == null) updatedAt = LocalDateTime.now();
        }

        @PreUpdate
        private void preUpdate() {
                normalize();
                updatedAt = LocalDateTime.now();
        }

        private void normalize() {
                if (email != null)     email = email.trim().toLowerCase();
                if (fullName != null)  fullName = fullName.trim();
                if (phone != null)     phone = phone.trim();
                if (avatarUrl != null) avatarUrl = avatarUrl.trim();
                if (address != null)   address = address.trim();
        }

        /* ======================
           Getters & Setters
           ====================== */
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public LocalDateTime getCreatedAt() { return createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
