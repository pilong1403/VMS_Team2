package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.request.OrgRegisterForm;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.User;
import com.fptuni.vms.service.AuthService;
import com.fptuni.vms.service.OrgRegisterBusinessService;
import com.fptuni.vms.service.OrganizationService;
import com.fptuni.vms.service.OrganizationService.OrgException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Service
@Transactional
public class OrgRegisterBusinessServiceImpl implements OrgRegisterBusinessService {

    private final CloudStorageService cloudStorageService;
    private final AuthService authService;
    private final OrganizationService organizationService;

    public OrgRegisterBusinessServiceImpl(CloudStorageService cloudStorageService,
                                          AuthService authService,
                                          OrganizationService organizationService) {
        this.cloudStorageService = cloudStorageService;
        this.authService = authService;
        this.organizationService = organizationService;
    }

    @Override
    public void finalizeRegistration(OrgRegisterForm form,
                                     byte[] regDocBytes, String regDocName, String regDocContentType,
                                     byte[] avatarBytes, String avatarName, String avatarContentType)
            throws OrgException {

        // ===== 1) Upload avatar nếu có, lấy URL (giữ nguyên logic cũ) =====
        String avatarUrl = null;
        if (avatarBytes != null && avatarBytes.length > 0) {
            MultipartFile avatarInMem =
                    new InMemMultipartFile("avatarFile", avatarName, avatarContentType, avatarBytes);
            avatarUrl = cloudStorageService.uploadFile(avatarInMem);
            if (avatarUrl == null) {
                throw new RuntimeException("Upload avatar thất bại.");
            }
        }

        // ===== 2) Tạo user ORG_OWNER (AuthServiceImpl đã set LOCKED) =====
        User owner = authService.registerOwnerAccount(
                form.getFullName(),
                form.getEmail(),
                form.getPhone(),
                form.getPassword(),
                form.getAddress(),
                avatarUrl // có thể null
        );

        // ===== 3) Upload tài liệu đăng ký =====
        MultipartFile inMem =
                new InMemMultipartFile("regDocFile", regDocName, regDocContentType, regDocBytes);
        String regDocUrl = cloudStorageService.uploadFile(inMem);
        if (regDocUrl == null) {
            throw new RuntimeException("Upload tài liệu thất bại.");
        }

        // ===== 4) Insert organizations (PENDING) =====
        organizationService.submitRegistration(
                owner,
                form.getOrgName(),
                form.getDescription(),
                regDocUrl,
                form.getRegNote()
        );
    }

    /**
     * MultipartFile in-memory – copy nguyên logic InMemFile từ controller,
     * chỉ đổi tên class và tách ra để dùng lại.
     */
    static final class InMemMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = (name == null ? "file" : name);
            this.originalFilename = (originalFilename == null ? "file" : originalFilename);
            this.contentType = contentType;
            this.content = (content == null ? new byte[0] : content);
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
