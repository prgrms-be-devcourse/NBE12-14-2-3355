package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;

public record RelatedGameResponse(
        Long id,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        BigDecimal recommendationScore
) {
}
