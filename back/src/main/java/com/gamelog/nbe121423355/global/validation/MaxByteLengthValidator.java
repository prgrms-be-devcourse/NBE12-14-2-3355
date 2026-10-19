package com.gamelog.nbe121423355.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxByteLengthValidator implements ConstraintValidator<MaxByteLength, String> {

    private int max;

    @Override
    public void initialize(MaxByteLength constraintAnnotation) {
        this.max = constraintAnnotation.value();
    }

    // null은 @NotBlank가 처리하므로 통과
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
