package com.fptuni.vms.enums;

/**
 * Tập mã lỗi chuẩn hoá cho nghiệp vụ xác thực/đăng ký.
 * Giúp tránh gõ sai chuỗi và dễ gợi ý khi code.
 */
public enum AuthErrorCode {
    // Đăng nhập
    USERNAME_PASSWORD_REQUIRED,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    SYSTEM_ERROR,

    // Đăng ký
    INVALID_INPUT,
    INVALID_EMAIL,
    EMAIL_EXISTS,
    WEAK_PASSWORD,
    ORG_PENDING,   // hồ sơ tổ chức đang chờ duyệt
    ORG_REJECTED;

    /** Trả về chuỗi mã (dùng cho query param, logging, v.v.). */
    public String code() {
        return name();
    }
}
