package com.fptuni.vms.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fptuni.vms.service.CloudinaryService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Validate file size (5MB max)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size cannot exceed 5MB");
        }

        try {
            // Generate unique public ID
            String publicId = "vms/avatars/" + UUID.randomUUID().toString();

            // Upload options
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "vms/avatars",
                    "resource_type", "image");

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadOptions);

            // Return secure URL
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new Exception("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String publicId) throws Exception {
        if (publicId == null || publicId.trim().isEmpty()) {
            return; // Nothing to delete
        }

        try {
            Map<String, Object> deleteResult = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap());

            String result = (String) deleteResult.get("result");
            if (!"ok".equals(result) && !"not found".equals(result)) {
                throw new Exception("Failed to delete image from Cloudinary: " + result);
            }

        } catch (IOException e) {
            throw new Exception("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // Extract public ID from Cloudinary URL
            // Example:
            // https://res.cloudinary.com/demo/image/upload/v1234567890/vms/avatars/uuid.jpg
            // Public ID: vms/avatars/uuid

            if (imageUrl.contains("/upload/")) {
                String[] parts = imageUrl.split("/upload/");
                if (parts.length > 1) {
                    String afterUpload = parts[1];
                    // Remove version if present (v1234567890/)
                    if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                        afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
                    }
                    // Remove file extension
                    int lastDotIndex = afterUpload.lastIndexOf(".");
                    if (lastDotIndex > 0) {
                        afterUpload = afterUpload.substring(0, lastDotIndex);
                    }
                    return afterUpload;
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }
}