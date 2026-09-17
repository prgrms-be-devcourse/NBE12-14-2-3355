package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DetailedReviewSaveRequest(
        @NotNull(message = "게임 기록은 필수입니다.")
        @Valid
        UserGameReqBody userGame,

        @NotNull(message = "리뷰는 필수입니다.")
        @Valid
        ReviewSaveRequest review
) {
}
