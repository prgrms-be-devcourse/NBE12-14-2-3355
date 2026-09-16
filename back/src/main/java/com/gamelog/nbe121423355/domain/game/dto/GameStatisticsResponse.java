package com.gamelog.nbe121423355.domain.game.dto;

public record GameStatisticsResponse(
        long playedCount,
        long playingCount,
        long backlogCount,
        long wishlistCount
) {
}
