package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatedGameResponse(
        Long id,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        BigDecimal recommendationScore,
        List<GameDetailResponse.GenreResponse> genres
) {
}
