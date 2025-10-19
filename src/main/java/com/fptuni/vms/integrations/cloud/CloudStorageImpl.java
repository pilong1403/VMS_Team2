package com.fptuni.vms.integrations.cloud;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CloudStorageImpl implements CloudStorageService {
    @Autowired
    private Cloudinary cloudinary;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "video/mp4",
            "video/quicktime", // for .mov files
            "application/msword", // for .doc
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // for .docx
    );

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            String contentType = file.getContentType();

            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType) || file.isEmpty()) {
                return null;
            }

            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", "auto");

            String originalFilename = file.getOriginalFilename();
            String publicId;
            String randomSuffix = UUID.randomUUID().toString().substring(0, 6);

            if (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.equals("application/pdf")) {
                String fileNameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                publicId = fileNameWithoutExtension + "_" + randomSuffix;
            } else {
                int lastDotIndex = originalFilename.lastIndexOf(".");
                String baseName = originalFilename.substring(0, lastDotIndex);
                String extension = originalFilename.substring(lastDotIndex);
                publicId = baseName + "_" + randomSuffix + extension;
            }

            options.put("public_id", publicId);

            Map result = cloudinary.uploader().upload(file.getBytes(), options);
            return (String) result.get("secure_url");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}