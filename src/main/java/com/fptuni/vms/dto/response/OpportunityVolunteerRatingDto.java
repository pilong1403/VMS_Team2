package com.fptuni.vms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpportunityVolunteerRatingDto {
    private Integer userId;
    private String userFullName;
    private String userAvatar;

    private Integer opportunityId;
    private String opportunityTitle;
    private String opportunityLocation;
    private LocalDateTime opportunityStart;
    private LocalDateTime opportunityEnd;

    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private BigDecimal totalHours;

    private Integer ratingId;   // null nếu chưa được đánh giá
    private Short stars;        // null nếu chưa được đánh giá
    private String comment;     // null nếu chưa được đánh giá
    private LocalDateTime ratingCreatedAt;

    private String status; // "NOT_ATTENDED", "PENDING", "RATED"

    public OpportunityVolunteerRatingDto(Integer userId, String userFullName, String userAvatar,
                                         Integer opportunityId, String opportunityTitle, String opportunityLocation,
                                         LocalDateTime opportunityStart, LocalDateTime opportunityEnd,
                                         LocalDateTime checkinTime, LocalDateTime checkoutTime, BigDecimal totalHours,
                                         Integer ratingId, Short stars, String comment, LocalDateTime ratingCreatedAt,
                                         String status) {
        this.userId = userId;
        this.userFullName = userFullName;
        this.userAvatar = userAvatar;
        this.opportunityId = opportunityId;
        this.opportunityTitle = opportunityTitle;
        this.opportunityLocation = opportunityLocation;
        this.opportunityStart = opportunityStart;
        this.opportunityEnd = opportunityEnd;
        this.checkinTime = checkinTime;
        this.checkoutTime = checkoutTime;
        this.totalHours = totalHours;
        this.ratingId = ratingId;
        this.stars = stars;
        this.comment = comment;
        this.ratingCreatedAt = ratingCreatedAt;
        this.status = status;
    }

    // GETTERS & SETTERS

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

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

    public String getOpportunityLocation() {
        return opportunityLocation;
    }

    public void setOpportunityLocation(String opportunityLocation) {
        this.opportunityLocation = opportunityLocation;
    }

    public LocalDateTime getOpportunityStart() {
        return opportunityStart;
    }

    public void setOpportunityStart(LocalDateTime opportunityStart) {
        this.opportunityStart = opportunityStart;
    }

    public LocalDateTime getOpportunityEnd() {
        return opportunityEnd;
    }

    public void setOpportunityEnd(LocalDateTime opportunityEnd) {
        this.opportunityEnd = opportunityEnd;
    }

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public LocalDateTime getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(LocalDateTime checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public Integer getRatingId() {
        return ratingId;
    }

    public void setRatingId(Integer ratingId) {
        this.ratingId = ratingId;
    }

    public Short getStars() {
        return stars;
    }

    public void setStars(Short stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getRatingCreatedAt() {
        return ratingCreatedAt;
    }

    public void setRatingCreatedAt(LocalDateTime ratingCreatedAt) {
        this.ratingCreatedAt = ratingCreatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
