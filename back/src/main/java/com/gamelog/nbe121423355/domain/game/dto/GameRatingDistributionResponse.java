package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;

// 특정 평점을 부여한 리뷰 수를 반환
public record GameRatingDistributionResponse(
        BigDecimal rating,
        long count
) {
}
