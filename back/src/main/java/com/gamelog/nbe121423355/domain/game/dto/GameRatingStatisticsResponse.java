package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

// 게임의 평균 평점, 리뷰 수, 평점별 분포를 반환
public record GameRatingStatisticsResponse(
        BigDecimal averageRating,
        long reviewCount,
        List<GameRatingDistributionResponse> ratingDistribution
) {
}
