package com.fptuni.vms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, PasswordConfirmation> {

    @Override
    public boolean isValid(PasswordConfirmation target, ConstraintValidatorContext context) {
        if (target == null) return true; // để @NotBlank trên field xử lý

        String pwd = target.getPassword();
        String confirm = target.getConfirmPassword();

        if (pwd == null || confirm == null) {
            // để @NotBlank / @NotNull lo message riêng
            return true;
        }

        boolean match = pwd.equals(confirm);
        if (!match) {
            // gắn lỗi vào field confirmPassword
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return match;
    }
}
