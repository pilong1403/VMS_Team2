package com.fptuni.vms.dto.request;

import com.fptuni.vms.model.Opportunity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class OpportunityForm {

    private Integer oppId;

    @NotNull(message = "Danh mục là bắt buộc")
    private Integer categoryId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 500, message = "Tiêu đề tối đa 500 ký tự")
    private String title;

    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
    private String subtitle;

    @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
    private String location;

    @NotNull(message = "Số tình nguyện viên cần là bắt buộc")
    @Min(value = 1, message = "Số tình nguyện viên cần tối thiểu là 1")
    @Max(value = 1000, message = "Số tình nguyện viên tối đa là 1000")
    private Integer neededVolunteers;

    @NotNull(message = "Trạng thái là bắt buộc")
    private Opportunity.OpportunityStatus status;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Giờ bắt đầu là bắt buộc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Ngày kết thúc là bắt buộc")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull(message = "Giờ kết thúc là bắt buộc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private MultipartFile thumbnailFile;
    private String thumbnailUrl;

    @Valid
    @NotNull(message = "Danh sách phần nội dung không được null")
    @Size(min = 1, message = "Cần ít nhất 1 phần nội dung")
    private List<OpportunitySectionForm> sections = new ArrayList<>();

    // Rule: kết thúc phải sau bắt đầu
    @AssertTrue(message = "Ngày/giờ kết thúc phải sau thời điểm bắt đầu")
    public boolean isEndAfterStart() {
        if (startDate == null || startTime == null || endDate == null || endTime == null) return true;
        LocalDateTime s = LocalDateTime.of(startDate, startTime);
        LocalDateTime e = LocalDateTime.of(endDate, endTime);
        return e.isAfter(s);
    }

    // Rule: thời điểm bắt đầu phải cách hiện tại ít nhất 24 giờ
    @AssertTrue(message = "Thời điểm bắt đầu phải sau ít nhất 24 giờ kể từ hiện tại")
    public boolean isStartNotInPast() {
        if (startDate == null || startTime == null) return true;
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime now = LocalDateTime.now();
        return !start.isBefore(now.plusHours(24));
    }

    // Getters & Setters
    public Integer getOppId() { return oppId; }
    public void setOppId(Integer oppId) { this.oppId = oppId; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getNeededVolunteers() { return neededVolunteers; }
    public void setNeededVolunteers(Integer neededVolunteers) { this.neededVolunteers = neededVolunteers; }
    public Opportunity.OpportunityStatus getStatus() { return status; }
    public void setStatus(Opportunity.OpportunityStatus status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public MultipartFile getThumbnailFile() { return thumbnailFile; }
    public void setThumbnailFile(MultipartFile thumbnailFile) { this.thumbnailFile = thumbnailFile; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public List<OpportunitySectionForm> getSections() { return sections; }
    public void setSections(List<OpportunitySectionForm> sections) { this.sections = sections; }
}
