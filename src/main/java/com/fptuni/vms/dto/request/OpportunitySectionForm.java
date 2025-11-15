package com.fptuni.vms.dto.request;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class OpportunitySectionForm {

    // validate thủ công trong validateBusinessRules()
    private Integer sectionOrder;

    //  giới hạn độ dài. Bắt buộc heading/content  làm trong validateBusinessRule.
    @Size(max = 255, message = "Tiêu đề phần tối đa 255 ký tự")
    private String heading;

    @Size(max = 10000, message = "Nội dung tối đa 10000 ký tự")
    private String content;

    @Size(max = 500, message = "URL hình ảnh tối đa 500 ký tự")
    private String imageUrl;

    private MultipartFile imageFile;

    @Size(max = 255, message = "Chú thích tối đa 255 ký tự")
    private String caption;

    public Integer getSectionOrder() { return sectionOrder; }
    public void setSectionOrder(Integer sectionOrder) { this.sectionOrder = sectionOrder; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}
