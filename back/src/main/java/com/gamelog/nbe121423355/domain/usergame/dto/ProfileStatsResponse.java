package com.gamelog.nbe121423355.domain.usergame.dto;

public record ProfileStatsResponse(
        long playedGameCount,
        double averageRating,
        long totalPlayTime
) {
}