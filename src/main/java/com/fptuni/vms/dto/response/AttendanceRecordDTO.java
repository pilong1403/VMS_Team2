package com.fptuni.vms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendanceRecordDTO {

    // --- Thông tin định danh ---
    private Integer applicationId;
    private Integer attendanceId; // Sẽ là null nếu chưa có bản ghi điểm danh

    // --- Thông tin Volunteer (từ User) ---
    private Integer volunteerId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;

    // --- Thông tin Điểm danh (từ Attendance) ---
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String status;
    private BigDecimal totalHours;
    private String notes;
    private String proofFileUrl;

    public AttendanceRecordDTO(Integer applicationId, Integer attendanceId, Integer volunteerId, String fullName, String email, String phone, String address, String avatarUrl, LocalDateTime checkinTime, LocalDateTime checkoutTime, String status, BigDecimal totalHours, String notes, String proofFileUrl) {
        this.applicationId = applicationId;
        this.attendanceId = attendanceId;
        this.volunteerId = volunteerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.checkinTime = checkinTime;
        this.checkoutTime = checkoutTime;
        // Nếu status từ DB là null (do LEFT JOIN), ta gán giá trị mặc định
        this.status = (status == null) ? "Chưa điểm danh" : status;
        this.totalHours = totalHours;
        this.notes = notes;
        this.proofFileUrl = proofFileUrl;
    }


    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getProofFileUrl() {
        return proofFileUrl;
    }

    public void setProofFileUrl(String proofFileUrl) {
        this.proofFileUrl = proofFileUrl;
    }
}