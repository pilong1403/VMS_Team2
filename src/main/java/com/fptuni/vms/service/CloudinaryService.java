package com.fptuni.vms.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String uploadImage(MultipartFile file) throws Exception;

    void deleteImage(String publicId) throws Exception;

    String extractPublicId(String imageUrl);
}