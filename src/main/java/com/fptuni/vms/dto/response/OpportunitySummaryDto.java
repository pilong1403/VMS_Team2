package com.fptuni.vms.dto.response;

import java.time.LocalDateTime;

public class OpportunitySummaryDto {
    private Integer opportunityId;
    private String opportunityTitle;
    private String location;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long participantCount;       // Số TNV đã điểm danh
    private Long ratedCount;             // Số TNV đã đánh giá
    private Long pendingRatingCount;     // Số TNV đã checkout nhưng chưa đánh giá

    private String status; // UPCOMING, ONGOING, FINISHED

    public OpportunitySummaryDto(Integer opportunityId, String opportunityTitle, String location,
                                 LocalDateTime startTime, LocalDateTime endTime,
                                 Long participantCount, Long ratedCount, Long pendingRatingCount,
                                 String status) {
        this.opportunityId = opportunityId;
        this.opportunityTitle = opportunityTitle;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantCount = participantCount;
        this.ratedCount = ratedCount;
        this.pendingRatingCount = pendingRatingCount;
        this.status = status;
    }

    // GETTERS & SETTERS

    public Integer getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Integer opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getOpportunityTitle() {
        return opportunityTitle;
    }

    public void setOpportunityTitle(String opportunityTitle) {
        this.opportunityTitle = opportunityTitle;
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

    public Long getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(Long participantCount) {
        this.participantCount = participantCount;
    }

    public Long getRatedCount() {
        return ratedCount;
    }

    public void setRatedCount(Long ratedCount) {
        this.ratedCount = ratedCount;
    }

    public Long getPendingRatingCount() {
        return pendingRatingCount;
    }

    public void setPendingRatingCount(Long pendingRatingCount) {
        this.pendingRatingCount = pendingRatingCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
