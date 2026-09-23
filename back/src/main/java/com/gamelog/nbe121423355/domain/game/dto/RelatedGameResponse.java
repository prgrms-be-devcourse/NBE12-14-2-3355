package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "연관 추천 게임")
public record RelatedGameResponse(
        @Schema(description = "GameLog 게임 ID", example = "9")
        Long id,
        @Schema(description = "게임명", example = "Baldur's Gate")
        String title,
        @Schema(description = "커버 이미지 URL", nullable = true)
        String coverImageUrl,
        @Schema(description = "IGDB 평점", example = "86.2", nullable = true)
        BigDecimal igdbRating,
        @Schema(description = "연관 추천 점수", example = "8.5")
        BigDecimal recommendationScore,
        @Schema(description = "장르 목록")
        List<GameDetailResponse.GenreResponse> genres
) {
}
