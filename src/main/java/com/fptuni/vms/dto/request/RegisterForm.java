package com.fptuni.vms.dto.request;

import com.fptuni.vms.validation.UniqueEmail;
import com.fptuni.vms.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự.")
    private String fullName;

    @Size(max = 100, message = "Email không vượt quá 100 ký tự.")
    @UniqueEmail(message = "Email đã tồn tại trong hệ thống. Vui lòng sử dụng tài khoản khác.")
    private String email;

    // BR-10: 10 chữ số, bắt đầu bằng 0 (VN)
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @ValidPassword(message = "Mật khẩu phải tối thiểu 8 ký tự và bao gồm chữ, số, và ký tự đặc biệt.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    // Validate chéo: password == confirmPassword
    @AssertTrue(message = "Mật khẩu xác nhận không khớp.")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
