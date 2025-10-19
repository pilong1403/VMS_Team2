// src/main/java/com/fptuni/vms/dto/request/OpportunityForm.java
package com.fptuni.vms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OpportunityForm {

    private Integer oppId; // null khi tạo mới

    @NotNull(message = "Organization is required")
    private Integer orgId; // sẽ gán = org của owner đang đăng nhập (không cho sửa trên form)

    @NotNull(message = "Category is required")
    private Integer categoryId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title too long")
    private String title;

    @Size(max = 500, message = "Subtitle too long")
    private String subtitle;

    @Size(max = 255, message = "Location too long")
    private String location;

    @Size(max = 500, message = "Thumbnail URL too long")
    private String thumbnailUrl;

    @NotNull(message = "Needed volunteers is required")
    @Min(value = 1, message = "Needed volunteers must be >= 1")
    private Integer neededVolunteers;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;
}
