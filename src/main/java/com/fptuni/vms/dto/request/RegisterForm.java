package com.fptuni.vms.dto.request;

import com.fptuni.vms.validation.UniqueEmail;
import com.fptuni.vms.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterForm {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @UniqueEmail(message = "Email already exists")
    private String email;

    // Theo BR-10: 10 digits, start with 0 (ví dụ VN)
    @Pattern(regexp = "^0\\d{9}$", message = "Phone must be 10 digits and start with 0")
    private String phone;

    @NotBlank(message = "Password is required")
    @ValidPassword // bạn đã có annotation này
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
