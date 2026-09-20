package com.gamelog.nbe121423355.domain.usergame.dto;

public record UserGameTasteDto(
        TasteMetricDto longPlay,
        TasteMetricDto rating,
        TasteMetricDto completion
) {
}
