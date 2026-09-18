package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DetailedReviewSaveRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void gameRecordWithoutReviewIsValid() {
        DetailedReviewSaveRequest request = new DetailedReviewSaveRequest(
                emptyUserGameRequest(),
                null
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void emptyReviewObjectIsValidAsGameRecordOnlyRequest() {
        DetailedReviewSaveRequest request = new DetailedReviewSaveRequest(
                emptyUserGameRequest(),
                new ReviewSaveRequest(null, "   ", false)
        );

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.review().hasReviewContent()).isFalse();
    }

    private UserGameReqBody emptyUserGameRequest() {
        return new UserGameReqBody(
                null,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
