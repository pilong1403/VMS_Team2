package com.fptuni.vms.validation;

import com.fptuni.vms.dto.request.RegisterForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterForm> {

    @Override
    public boolean isValid(RegisterForm form, ConstraintValidatorContext context) {
        if (form.getPassword() == null || form.getConfirmPassword() == null) {
            return true; // để NotBlank xử lý riêng
        }

        boolean matched = form.getPassword().equals(form.getConfirmPassword());

        if (!matched) {
            // Gắn lỗi vào trường confirmPassword thay vì global error
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return matched;
    }
}
