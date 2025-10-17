package com.fptuni.vms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return false; // đã có @NotBlank, nhưng để chắc chắn

        if (value.length() < 8) return false;

        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c))    hasDigit = true;

            if (hasUpper && hasLower && hasDigit) return true;
        }
        return false;
    }
}
