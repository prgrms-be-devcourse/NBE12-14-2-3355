package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record UserGameTasteResponse(
        BigDecimal longPlayRatio,
        String longPlayMessage,

        BigDecimal highRatingRatio,
        String highRatingMessage,

        BigDecimal completionRatio,
        String completionMessage
) {
}
