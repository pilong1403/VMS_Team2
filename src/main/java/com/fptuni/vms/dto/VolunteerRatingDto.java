package com.fptuni.vms.dto;

public class VolunteerRatingDto {
    private Double averageStars;
    private Integer totalRatings;

    // Constructors
    public VolunteerRatingDto() {
    }

    public VolunteerRatingDto(Double averageStars, Integer totalRatings) {
        this.averageStars = averageStars;
        this.totalRatings = totalRatings;
    }

    // Getters and Setters
    public Double getAverageStars() {
        return averageStars;
    }

    public void setAverageStars(Double averageStars) {
        this.averageStars = averageStars;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }
}