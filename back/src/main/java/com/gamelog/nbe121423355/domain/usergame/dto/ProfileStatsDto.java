package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record ProfileStatsDto(
        long playedGameCount,
        BigDecimal averageRating,
        BigDecimal totalPlayTime
) {
}