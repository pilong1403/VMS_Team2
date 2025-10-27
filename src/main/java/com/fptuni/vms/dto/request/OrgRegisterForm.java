package com.fptuni.vms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class OrgRegisterForm {

    @NotBlank(message = "Vui lòng nhập tên tổ chức.")
    @Size(max = 200, message = "Tên tổ chức không vượt quá 200 ký tự.")
    private String orgName;

    @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự.")
    private String description;

    @Size(max = 500, message = "Ghi chú tài liệu không vượt quá 500 ký tự.")
    private String regNote;

    @NotNull(message = "Vui lòng tải tài liệu đăng ký.")
    private MultipartFile regDocFile;

    // NEW: Avatar tổ chức (tùy chọn, dạng file)
    private MultipartFile avatarFile;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự.")
    private String fullName;

    @Size(max = 100, message = "Email không vượt quá 100 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự.")
    @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @Size(max = 500, message = "Địa chỉ không vượt quá 500 ký tự.")
    private String address;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;
}
