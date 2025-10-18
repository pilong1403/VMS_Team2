package com.fptuni.vms.dto.view;

import com.fptuni.vms.model.Opportunity;
import java.time.LocalDateTime;

/**
 * DTO cho hiển thị card cơ hội tình nguyện trên trang home
 */
public class OpportunityCardDto {

    private Integer oppId;
    private String title;
    private String subtitle;
    private String location;
    private String thumbnailUrl;
    private String organizationName;
    private boolean organizationVerified;
    private String categoryName;
    private Opportunity.OpportunityStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer neededVolunteers;
    private Integer appliedVolunteers;
    private LocalDateTime createdAt;

    // Constructor
    public OpportunityCardDto() {
    }

    public OpportunityCardDto(Integer oppId, String title, String subtitle, String location,
            String thumbnailUrl, String organizationName, boolean organizationVerified,
            String categoryName, Opportunity.OpportunityStatus status,
            LocalDateTime startTime, LocalDateTime endTime,
            Integer neededVolunteers, Integer appliedVolunteers, LocalDateTime createdAt) {
        this.oppId = oppId;
        this.title = title;
        this.subtitle = subtitle;
        this.location = location;
        this.thumbnailUrl = thumbnailUrl;
        this.organizationName = organizationName;
        this.organizationVerified = organizationVerified;
        this.categoryName = categoryName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.neededVolunteers = neededVolunteers;
        this.appliedVolunteers = appliedVolunteers;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getOppId() {
        return oppId;
    }

    public void setOppId(Integer oppId) {
        this.oppId = oppId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public boolean isOrganizationVerified() {
        return organizationVerified;
    }

    public void setOrganizationVerified(boolean organizationVerified) {
        this.organizationVerified = organizationVerified;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Opportunity.OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(Opportunity.OpportunityStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNeededVolunteers() {
        return neededVolunteers;
    }

    public void setNeededVolunteers(Integer neededVolunteers) {
        this.neededVolunteers = neededVolunteers;
    }

    public Integer getAppliedVolunteers() {
        return appliedVolunteers;
    }

    public void setAppliedVolunteers(Integer appliedVolunteers) {
        this.appliedVolunteers = appliedVolunteers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public int getProgressPercentage() {
        if (neededVolunteers == null || neededVolunteers == 0)
            return 0;
        if (appliedVolunteers == null)
            return 0;
        return Math.min(100, (appliedVolunteers * 100) / neededVolunteers);
    }

    public String getStatusDisplayName() {
        if (status == null)
            return "Không xác định";
        switch (status) {
            case OPEN:
                return "Đang mở";
            case CLOSED:
                return "Đã đóng";
            case CANCELLED:
                return "Đã hủy";
            default:
                return status.name();
        }
    }

    public String getStatusBadgeClass() {
        if (status == null)
            return "bg-secondary";
        switch (status) {
            case OPEN:
                return "bg-success";
            case CLOSED:
                return "bg-warning";
            case CANCELLED:
                return "bg-danger";
            default:
                return "bg-secondary";
        }
    }
}