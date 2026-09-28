package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.review.validation.ValidRating;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ReviewSaveRequest(
        @Schema(description = "별점. 입력하지 않을 수 있으며 0.5점 단위입니다.", example = "4.5", nullable = true)
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        @ValidRating
        BigDecimal rating,

        @Schema(description = "리뷰 내용. 별점만 저장하는 경우 생략할 수 있습니다.", example = "스토리와 전투가 재미있어요.", nullable = true)
        String content,

        @Schema(description = "스포일러 포함 여부", example = "false")
        boolean spoiler
) {
    public boolean hasReviewContent() {
        return rating != null || (content != null && !content.isBlank());
    }
}
