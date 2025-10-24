package com.fptuni.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "categories",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_categories_category_name", columnNames = "category_name")
        }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    // DB timestamps (DEFAULT/trigger) -> read-only in JPA
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (categoryName != null) categoryName = categoryName.trim();
        if (description != null) description = description.trim();
    }

    // Getters & Setters
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
