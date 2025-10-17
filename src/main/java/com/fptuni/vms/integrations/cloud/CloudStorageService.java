package com.fptuni.vms.integrations.cloud;

import org.springframework.web.multipart.MultipartFile;

public interface CloudStorageService {
    /** Upload file, trả về URL công khai */
    String upload(MultipartFile file, String folder, String publicId);

    /** Xoá theo publicId */
    void delete(String publicId);

    /** Trả về URL đã ký (nếu cần tải riêng tư hoặc resize) */
    String buildUrl(String publicId, Integer width, Integer height, boolean signed);
}
