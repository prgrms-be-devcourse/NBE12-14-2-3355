package com.gamelog.nbe121423355.domain.review.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSaveRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void contentOnlyReviewIsValid() {
        ReviewSaveRequest request = new ReviewSaveRequest(
                null,
                "별점 없이 작성한 리뷰",
                false
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void ratingOnlyReviewIsValid() {
        ReviewSaveRequest request = new ReviewSaveRequest(
                new BigDecimal("4.5"),
                null,
                false
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void emptyReviewHasNoReviewContent() {
        ReviewSaveRequest request = new ReviewSaveRequest(
                null,
                "   ",
                false
        );

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.hasReviewContent()).isFalse();
    }
}
