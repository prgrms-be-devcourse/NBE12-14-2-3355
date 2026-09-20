package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record UserGameScatterDto(
        Long gameId,
        String title,
        String coverImage,
        BigDecimal playTime,
        BigDecimal rating
) {
}
