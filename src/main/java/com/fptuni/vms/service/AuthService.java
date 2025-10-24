package com.fptuni.vms.service;

import com.fptuni.vms.enums.AuthErrorCode;
import com.fptuni.vms.model.User;

/**
 * AuthService: xác thực & đăng ký tài khoản.
 * - Login: ném AuthException với code chuẩn hoá cho UI map ra thông báo.
 * - Register: validate input, check trùng email, gán role VOLUNTEER, mã hoá mật khẩu.
 */
public interface AuthService {

    /** Exception mang theo AuthErrorCode để Controller redirect /login?e=<code>. */
    class AuthException extends RuntimeException {
        private final AuthErrorCode code;

        public AuthException(AuthErrorCode code) {
            super(code != null ? code.name() : null);
            this.code = code;
        }

        public AuthException(AuthErrorCode code, String message) {
            super(message);
            this.code = code;
        }

        public AuthException(AuthErrorCode code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public AuthErrorCode getCode() {
            return code;
        }

        /** Tiện ích: trả về chuỗi mã lỗi (dùng cho redirect hoặc log). */
        public String getCodeString() {
            return code != null ? code.code() : null;
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
     * Ném AuthException với một trong các mã lỗi AuthErrorCode.
     */
    User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException;
}
