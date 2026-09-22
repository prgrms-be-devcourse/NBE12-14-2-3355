package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

// 게임의 평균 평점, 평가 수, 내용이 있는 리뷰 수와 평점별 분포를 반환
public record GameRatingStatisticsResponse(
        BigDecimal averageRating,
        long ratingCount,
        long reviewCount,
        List<GameRatingDistributionResponse> ratingDistribution
) {
}
