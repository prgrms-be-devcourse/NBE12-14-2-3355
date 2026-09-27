package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

// 특정 평점을 부여한 리뷰 수를 반환
@Schema(description = "별점별 리뷰 분포")
public record GameRatingDistributionResponse(
        @Schema(description = "별점", example = "4.5")
        BigDecimal rating,
        @Schema(description = "해당 별점 리뷰 수", example = "12")
        long count
) {
}
