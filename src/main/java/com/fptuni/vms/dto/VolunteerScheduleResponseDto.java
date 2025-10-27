package com.fptuni.vms.dto;

import java.util.List;

public class VolunteerScheduleResponseDto {
    private List<ScheduleApplicationDto> upcomingApplications;
    private List<ScheduleApplicationDto> pastApplications;
    private int upcomingCount;
    private int completedCount;
    private long totalHours;

    // Default constructor
    public VolunteerScheduleResponseDto() {
    }

    // Constructor with all fields
    public VolunteerScheduleResponseDto(List<ScheduleApplicationDto> upcomingApplications,
            List<ScheduleApplicationDto> pastApplications,
            int upcomingCount, int completedCount, long totalHours) {
        this.upcomingApplications = upcomingApplications;
        this.pastApplications = pastApplications;
        this.upcomingCount = upcomingCount;
        this.completedCount = completedCount;
        this.totalHours = totalHours;
    }

    // Getters and Setters
    public List<ScheduleApplicationDto> getUpcomingApplications() {
        return upcomingApplications;
    }

    public void setUpcomingApplications(List<ScheduleApplicationDto> upcomingApplications) {
        this.upcomingApplications = upcomingApplications;
    }

    public List<ScheduleApplicationDto> getPastApplications() {
        return pastApplications;
    }

    public void setPastApplications(List<ScheduleApplicationDto> pastApplications) {
        this.pastApplications = pastApplications;
    }

    public int getUpcomingCount() {
        return upcomingCount;
    }

    public void setUpcomingCount(int upcomingCount) {
        this.upcomingCount = upcomingCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public long getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(long totalHours) {
        this.totalHours = totalHours;
    }
}