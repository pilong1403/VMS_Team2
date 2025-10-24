package com.fptuni.vms.dto.request;

import com.fptuni.vms.model.Opportunity;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OpportunityForm {

    private Integer oppId; // null khi tạo mới

    @NotNull
    private Integer categoryId;

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 500)
    private String subtitle;

    @Size(max = 255)
    @Pattern(regexp=".*\\S.*", message="Địa điểm không được để trống hoàn toàn")
    private String location;

    // Có thể upload qua Cloudinary -> set thumbnailUrl mới
    @Size(max = 500)
    private String thumbnailUrl;

    @NotNull
    @Min(1)
    private Integer neededVolunteers;

    /**
     * Enum Opportunity.OpportunityStatus (OPEN/CLOSED/CANCELLED)
     * => khớp DB CHECK constraint + mặc định OPEN.
     */
    @NotNull
    private Opportunity.OpportunityStatus status = Opportunity.OpportunityStatus.OPEN;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    /** Không có constraint trong DB, giới hạn hợp lý để tránh gửi quá nhiều */
    @Size(max = 50)
    private List<SectionForm> sections = new ArrayList<>();

    // ===== helpers =====
    public boolean isTimeValid() {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }

    // ===== getters/setters =====
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

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getNeededVolunteers() { return neededVolunteers; }
    public void setNeededVolunteers(Integer neededVolunteers) { this.neededVolunteers = neededVolunteers; }

    public Opportunity.OpportunityStatus getStatus() { return status; }
    public void setStatus(Opportunity.OpportunityStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public List<SectionForm> getSections() { return sections; }
    public void setSections(List<SectionForm> sections) { this.sections = sections; }

    // ===== Nested section form =====
    public static class SectionForm {
        private Integer sectionId; // dùng khi edit

        @NotNull
        private Integer sectionOrder;

        @Size(max = 255)
        private String heading;

        private String content; // NVARCHAR(MAX)

        @Size(max = 500)
        private String imageUrl;

        @Size(max = 255)
        private String caption;

        public Integer getSectionId() { return sectionId; }
        public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }

        public Integer getSectionOrder() { return sectionOrder; }
        public void setSectionOrder(Integer sectionOrder) { this.sectionOrder = sectionOrder; }

        public String getHeading() { return heading; }
        public void setHeading(String heading) { this.heading = heading; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
    }
}
