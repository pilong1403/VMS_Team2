// src/main/java/com/fptuni/vms/service/AuthService.java
package com.fptuni.vms.service;

import com.fptuni.vms.model.User;

/**
 * AuthService: xác thực & đăng ký tài khoản.
 * - Login: ném AuthException với code chuẩn hoá cho UI map ra thông báo.
 * - Register: validate input, check trùng email, gán role VOLUNTEER, mã hoá mật khẩu.
 */
public interface AuthService {

    /**
     * Mã lỗi chuẩn hoá dùng cho UI (AuthController.map(...)).
     * Có thể bổ sung thêm nếu bạn mở rộng tính năng.
     */
    interface ErrorCode {
        // Đăng nhập
        String USERNAME_PASSWORD_REQUIRED = "USERNAME_PASSWORD_REQUIRED";
        String INVALID_CREDENTIALS        = "INVALID_CREDENTIALS";
        String ACCOUNT_LOCKED             = "ACCOUNT_LOCKED";
        String SYSTEM_ERROR               = "SYSTEM_ERROR";

        // Đăng ký
        String INVALID_INPUT              = "INVALID_INPUT";
        String INVALID_EMAIL              = "INVALID_EMAIL";
        String EMAIL_EXISTS               = "EMAIL_EXISTS";
        String WEAK_PASSWORD              = "WEAK_PASSWORD";
    }

    /**
     * Exception mang theo "code" để Controller redirect /login?e=<code>.
     * LƯU Ý: AuthController đang gọi ex.getCode(), nên cần getter này.
     */
    class AuthException extends RuntimeException {
        private final String code;

        public AuthException(String code) {
            super(code);
            this.code = code;
        }

        public AuthException(String code, String message) {
            super(message);
            this.code = code;
        }

        public AuthException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /** Đăng nhập bằng email + mật khẩu (mật khẩu đã mã hoá trong DB). */
    User login(String email, String rawPassword) throws AuthException;

    /**
     * Đăng ký mới volunteer:
     * - Validate họ tên/email/mật khẩu
     * - Kiểm tra trùng email
     * - Gán role = VOLUNTEER
     * - Mã hoá mật khẩu
     * - Trả về User đã lưu
     * Ném AuthException với một trong các code:
     *   INVALID_INPUT, INVALID_EMAIL, EMAIL_EXISTS, WEAK_PASSWORD, SYSTEM_ERROR
     */
    User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException;
}
