package com.gamelog.nbe121423355.domain.review.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class RatingValidator implements ConstraintValidator<ValidRating, BigDecimal> {

    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    @Override
    public boolean isValid(BigDecimal rating, ConstraintValidatorContext context) {
        // null은 @NotNull에서 검사.
        if (rating == null) {
            return true;
        }

        return rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) == 0;
    }
}
