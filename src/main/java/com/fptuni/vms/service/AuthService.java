// src/main/java/com/fptuni/vms/service/AuthService.java
package com.fptuni.vms.service;

import com.fptuni.vms.enums.AuthErrorCode;
import com.fptuni.vms.model.User;

public interface AuthService {

    class AuthException extends RuntimeException {
        private final AuthErrorCode code;
        public AuthException(AuthErrorCode code) { super(code != null ? code.name() : null); this.code = code; }
        public AuthException(AuthErrorCode code, String message) { super(message); this.code = code; }
        public AuthException(AuthErrorCode code, String message, Throwable cause) { super(message, cause); this.code = code; }
        public AuthErrorCode getCode() { return code; }
        public String getCodeString() { return code != null ? code.code() : null; }
    }

    User login(String email, String rawPassword) throws AuthException;
    User registerVolunteer(String fullName, String email, String phone, String rawPassword) throws AuthException;
    User registerOwnerAccount(String fullName, String email, String phone, String rawPassword,
                              String address, String avatarUrl) throws AuthException;

    /** NGHIỆP VỤ: xác thực email có thể dùng để mở tài khoản mới (không trống, đúng định dạng, chưa tồn tại) */
    void assertNewAccountEmailUsable(String email) throws AuthException;
}
