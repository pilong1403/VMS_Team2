package com.fptuni.vms.dto.request;

import com.fptuni.vms.validation.UniqueEmail;
import com.fptuni.vms.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters") // đổi 50 nếu DB để 50
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    @UniqueEmail(message = "Email already exists")
    private String email;

    // BR-10: 10 digits, start with 0 (VN)
    @Pattern(regexp = "^0\\d{9}$", message = "Phone must be 10 digits and start with 0")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @ValidPassword // đã có annotation này (gợi ý: min 8, có số/chữ/ký tự đặc biệt)
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
