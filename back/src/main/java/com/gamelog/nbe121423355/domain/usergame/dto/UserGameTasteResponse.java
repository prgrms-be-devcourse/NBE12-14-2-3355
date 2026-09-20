package com.gamelog.nbe121423355.domain.usergame.dto;

public record UserGameTasteResponse(
        TasteMetricResponse longPlay,
        TasteMetricResponse rating,
        TasteMetricResponse completion
) {
}
