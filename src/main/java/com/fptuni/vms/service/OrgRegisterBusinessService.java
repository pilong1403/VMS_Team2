package com.fptuni.vms.service;

import com.fptuni.vms.dto.request.OrgRegisterForm;
import com.fptuni.vms.service.OrganizationService.OrgException;

public interface OrgRegisterBusinessService {

    /**
     * Hoàn tất luồng đăng ký tổ chức sau khi OTP đã verify OK.
     * Logic:
     *  - Upload avatar (nếu có)
     *  - Tạo ORG_OWNER (User, status = LOCKED)
     *  - Upload tài liệu đăng ký
     *  - submitRegistration (org PENDING)
     */
    void finalizeRegistration(OrgRegisterForm form,
                              byte[] regDocBytes, String regDocName, String regDocContentType,
                              byte[] avatarBytes, String avatarName, String avatarContentType)
            throws OrgException;
}
