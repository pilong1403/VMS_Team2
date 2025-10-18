package com.fptuni.vms.integrations.cloud;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService implements CloudStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String upload(MultipartFile file, String folder, String publicId) {
        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "auto",        // ảnh/video/pdf đều ok
                    "overwrite", true
            );
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            Object status = result.get("result");
            if (!"ok".equals(status)) {
                throw new RuntimeException("Cloudinary destroy failed: " + status);
            }
        } catch (IOException e) {
            throw new RuntimeException("Delete on Cloudinary failed", e);
        }
    }

    @Override
    public String buildUrl(String publicId, Integer width, Integer height, boolean signed) {
        var builder = cloudinary.url()
                .secure(true)
                .publicId(publicId);
        if (width != null && height != null) {
            builder = builder.transformation(
                    new com.cloudinary.Transformation().width(width).height(height).crop("fill")
            );
        }
        if (signed) builder = builder.signed(true);
        return builder.generate(publicId);
    }
}
