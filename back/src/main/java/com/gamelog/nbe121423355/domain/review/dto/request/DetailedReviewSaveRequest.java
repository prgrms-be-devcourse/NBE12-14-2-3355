package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DetailedReviewSaveRequest(
        @Schema(description = "저장할 게임 기록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "게임 기록은 필수입니다.")
        @Valid
        UserGameReqBody userGame,

        @Schema(description = "저장할 리뷰. 게임 기록만 저장할 때는 null입니다.", nullable = true)
        @Valid
        ReviewSaveRequest review
) {
}
