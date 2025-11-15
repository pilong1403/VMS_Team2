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

    // Service upload file (Cloudinary, S3, ... tuỳ implement)
    private final CloudStorageService cloudStorageService;
    // Service xử lý account (registerOwnerAccount, validate email, ...)
    private final AuthService authService;
    // Service thao tác bảng organizations (submitRegistration, search, ...)
    private final OrganizationService organizationService;

    public OrgRegisterBusinessServiceImpl(CloudStorageService cloudStorageService,
                                          AuthService authService,
                                          OrganizationService organizationService) {
        this.cloudStorageService = cloudStorageService;
        this.authService = authService;
        this.organizationService = organizationService;
    }

    /**
     * finalizeRegistration:
     *  - Được gọi sau khi OTP đã verify OK (ở controller).
     *  - Nhiệm vụ:
     *      1) Upload avatar (nếu có) -> lấy URL
     *      2) Tạo user ORG_OWNER (status = LOCKED) bằng AuthService
     *      3) Upload tài liệu đăng ký (regDoc) -> lấy URL
     *      4) Gọi OrganizationService.submitRegistration(...) để tạo / cập nhật org PENDING
     *
     *  - Dùng @Transactional ở class-level => nếu bất kỳ bước nào ném RuntimeException
     *    hoặc OrgException (checked nhưng được khai báo throws) thì transaction sẽ rollback
     *    (tùy config, nhưng đây là ý tưởng).
     */
    @Override
    public void finalizeRegistration(OrgRegisterForm form,
                                     byte[] regDocBytes, String regDocName, String regDocContentType,
                                     byte[] avatarBytes, String avatarName, String avatarContentType)
            throws OrgException {

        // ===== 1) Upload avatar nếu có, lấy URL (giữ nguyên logic cũ) =====
        // avatarBytes được controller lưu trong session, giờ chuyển thành MultipartFile in-memory
        String avatarUrl = null;
        if (avatarBytes != null && avatarBytes.length > 0) {
            // InMemMultipartFile: implement MultipartFile, nhưng không đọc từ disk,
            // mà đọc từ mảng byte đã có sẵn trong RAM.
            MultipartFile avatarInMem =
                    new InMemMultipartFile("avatarFile", avatarName, avatarContentType, avatarBytes);

            // Upload lên CloudStorage, implement cụ thể tuỳ hệ thống (Cloudinary, S3,...)
            avatarUrl = cloudStorageService.uploadFile(avatarInMem);

            // Nếu service trả null => coi như lỗi upload
            if (avatarUrl == null) {
                // Dùng RuntimeException để transaction rollback và controller catch(Exception) xử lý
                throw new RuntimeException("Upload avatar thất bại.");
            }
        }

        // ===== 2) Tạo user ORG_OWNER (AuthServiceImpl đã set LOCKED) =====
        //  - Sử dụng thông tin từ form để tạo account mới cho chủ tổ chức.
        //  - registerOwnerAccount có logic:
        //      + validate email, password
        //      + xử lý case email đã tồn tại nhưng org REJECTED => cho phép đăng ký lại
        //      + set role ORG_OWNER, status = LOCKED
        User owner = authService.registerOwnerAccount(
                form.getFullName(),
                form.getEmail(),
                form.getPhone(),
                form.getPassword(),
                form.getAddress(),
                avatarUrl // có thể null nếu user không upload avatar
        );

        // ===== 3) Upload tài liệu đăng ký (regDoc) =====
        // Tương tự avatar, nhưng đây là file tài liệu (PDF/Word/ảnh).
        MultipartFile inMem =
                new InMemMultipartFile("regDocFile", regDocName, regDocContentType, regDocBytes);

        String regDocUrl = cloudStorageService.uploadFile(inMem);
        if (regDocUrl == null) {
            throw new RuntimeException("Upload tài liệu đăng ký thất bại.");
        }

        // ===== 4) Insert organizations (PENDING) =====
        // submitRegistration có nhiệm vụ:
        //  - Nếu owner đã có org:
        //      + PENDING/APPROVED => chặn (owner đã có org)
        //      + REJECTED => cho phép update hồ sơ, set lại PENDING
        //  - Nếu chưa có org:
        //      + Tạo bản ghi mới với RegStatus = PENDING
        organizationService.submitRegistration(
                owner,
                form.getOrgName(),
                form.getDescription(),
                regDocUrl,
                form.getRegNote()
        );
    }

    /**
     * InMemMultipartFile:
     *  - Đây là implement của MultipartFile mà dữ liệu nằm toàn bộ trong RAM (mảng byte `content`).
     *  - Dùng để "wrap" dữ liệu file lấy từ session (byte[]) thành MultipartFile,
     *    sau đó truyền cho CloudStorageService.uploadFile(...) mà không cần lưu ra file tạm trên disk.
     *
     *  - Ưu điểm:
     *      + Không phải tạo file tạm => đơn giản, tránh IO thừa.
     *      + API upload CloudStorage đã quen làm việc với MultipartFile.
     *
     *  - Nhược điểm:
     *      + Không phù hợp nếu file quá lớn (nhưng ở đây đã giới hạn size ở controller).
     */
    static final class InMemMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            // Nếu name/originalFilename null thì gán "file" cho an toàn (tránh NullPointer ở chỗ khác)
            this.name = (name == null ? "file" : name);
            this.originalFilename = (originalFilename == null ? "file" : originalFilename);
            this.contentType = contentType;
            // Nếu content null thì dùng mảng rỗng để tránh NullPointer khi getBytes()
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
            // clone() để tránh code ở ngoài sửa trực tiếp mảng nội bộ
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            // Bọc content thành ByteArrayInputStream để code upload có thể stream đọc
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            // Nếu cần ghi ra file (ít dùng trong flow này), thì ghi toàn bộ content ra disk
            Files.write(dest.toPath(), content);
        }
    }
}
