package com.fptuni.vms.dto;

import java.time.LocalDateTime;

public class EventHistoryDto {
    private Integer appId;
    private Integer oppId;
    private String opportunityTitle;
    private String organizationName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String thumbnailUrl;
    private String subtitle;
    private boolean hasAttended;
    private boolean hasRated;
    private Integer existingFeedbackId;
    private Integer existingRating;
    private String existingContent;

    // Organization rating about volunteer
    private boolean hasOrgRating;
    private Short orgRatingStars;
    private String orgRatingComment;

    // Default constructor
    public EventHistoryDto() {
    }

    // Constructor with all fields
    public EventHistoryDto(Integer appId, Integer oppId, String opportunityTitle, String organizationName,
            String location, LocalDateTime startTime, LocalDateTime endTime, String thumbnailUrl,
            String subtitle, boolean hasAttended, boolean hasRated, Integer existingFeedbackId,
            Integer existingRating, String existingContent, boolean hasOrgRating,
            Short orgRatingStars, String orgRatingComment) {
        this.appId = appId;
        this.oppId = oppId;
        this.opportunityTitle = opportunityTitle;
        this.organizationName = organizationName;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.thumbnailUrl = thumbnailUrl;
        this.subtitle = subtitle;
        this.hasAttended = hasAttended;
        this.hasRated = hasRated;
        this.existingFeedbackId = existingFeedbackId;
        this.existingRating = existingRating;
        this.existingContent = existingContent;
        this.hasOrgRating = hasOrgRating;
        this.orgRatingStars = orgRatingStars;
        this.orgRatingComment = orgRatingComment;
    }

    // Getters and Setters
    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Integer getOppId() {
        return oppId;
    }

    public void setOppId(Integer oppId) {
        this.oppId = oppId;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public boolean isHasAttended() {
        return hasAttended;
    }

    public void setHasAttended(boolean hasAttended) {
        this.hasAttended = hasAttended;
    }

    public boolean isHasRated() {
        return hasRated;
    }

    public void setHasRated(boolean hasRated) {
        this.hasRated = hasRated;
    }

    public Integer getExistingFeedbackId() {
        return existingFeedbackId;
    }

    public void setExistingFeedbackId(Integer existingFeedbackId) {
        this.existingFeedbackId = existingFeedbackId;
    }

    public Integer getExistingRating() {
        return existingRating;
    }

    public void setExistingRating(Integer existingRating) {
        this.existingRating = existingRating;
    }

    public String getExistingContent() {
        return existingContent;
    }

    public void setExistingContent(String existingContent) {
        this.existingContent = existingContent;
    }

    public boolean isHasOrgRating() {
        return hasOrgRating;
    }

    public void setHasOrgRating(boolean hasOrgRating) {
        this.hasOrgRating = hasOrgRating;
    }

    public Short getOrgRatingStars() {
        return orgRatingStars;
    }

    public void setOrgRatingStars(Short orgRatingStars) {
        this.orgRatingStars = orgRatingStars;
    }

    public String getOrgRatingComment() {
        return orgRatingComment;
    }

    public void setOrgRatingComment(String orgRatingComment) {
        this.orgRatingComment = orgRatingComment;
    }
}