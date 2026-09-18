package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record UserGameScatterResponse(
        Long gameId,
        String title,
        String coverImage,
        BigDecimal playTime,
        BigDecimal rating
) {
}
