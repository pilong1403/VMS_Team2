package com.fptuni.vms.dto.request;

import com.fptuni.vms.validation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@PasswordMatches
public class RegisterForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự.")
    private String fullName;

    @Size(max = 100, message = "Email không vượt quá 100 ký tự.")
    @UniqueEmail(message = "Email đã tồn tại trong hệ thống. Vui lòng sử dụng tài khoản khác.")
    private String email;

    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @ValidPassword(message = "Mật khẩu phải tối thiểu 8 ký tự và bao gồm chữ, số, và ký tự đặc biệt.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;
}
