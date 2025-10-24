package com.fptuni.vms.dto;

import java.time.LocalDateTime;

public class ScheduleApplicationDto {
    private Integer appId;
    private String opportunityTitle;
    private String organizationName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime appliedAt;
    private String status;
    private Integer neededVolunteers;
    private String description;
    private String thumbnailUrl;

    // Default constructor
    public ScheduleApplicationDto() {
    }

    // Constructor with all fields
    public ScheduleApplicationDto(Integer appId, String opportunityTitle, String organizationName,
            String location, LocalDateTime startTime, LocalDateTime endTime,
            LocalDateTime appliedAt, String status, Integer neededVolunteers,
            String description, String thumbnailUrl) {
        this.appId = appId;
        this.opportunityTitle = opportunityTitle;
        this.organizationName = organizationName;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.appliedAt = appliedAt;
        this.status = status;
        this.neededVolunteers = neededVolunteers;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Getters and Setters
    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getOpportunityTitle() {
        return opportunityTitle;
    }

    public void setOpportunityTitle(String opportunityTitle) {
        this.opportunityTitle = opportunityTitle;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNeededVolunteers() {
        return neededVolunteers;
    }

    public void setNeededVolunteers(Integer neededVolunteers) {
        this.neededVolunteers = neededVolunteers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}