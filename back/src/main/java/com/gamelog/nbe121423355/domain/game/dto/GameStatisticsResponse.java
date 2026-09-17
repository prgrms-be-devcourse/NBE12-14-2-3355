package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

public record GameStatisticsResponse(
        long playedCount,
        long playingCount,
        long backlogCount,
        long wishlistCount,
        long likeCount,
        BigDecimal averageRating,
        long reviewCount,
        List<GameRatingDistributionResponse> ratingDistribution,
        BigDecimal averagePlayTimeHours,
        long playTimeUserCount
) {
}
