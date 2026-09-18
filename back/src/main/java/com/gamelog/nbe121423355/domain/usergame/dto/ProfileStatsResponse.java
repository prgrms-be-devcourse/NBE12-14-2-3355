package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record ProfileStatsResponse(
        long playedGameCount,
        BigDecimal averageRating,
        BigDecimal totalPlayTime
) {
}