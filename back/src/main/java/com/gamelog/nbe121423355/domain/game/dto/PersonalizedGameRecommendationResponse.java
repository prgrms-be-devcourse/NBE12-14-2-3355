package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

// 사용자 맞춤 추천 게임의 기본 정보, 추천 점수, 장르를 반환
@Schema(description = "사용자 맞춤 추천 게임")
public record PersonalizedGameRecommendationResponse(
        @Schema(description = "GameLog 게임 ID", example = "6")
        Long id,
        @Schema(description = "게임명", example = "Baldur's Gate II: Shadows of Amn")
        String title,
        @Schema(description = "커버 이미지 URL", nullable = true)
        String coverImageUrl,
        @Schema(description = "IGDB 평점", example = "89.5", nullable = true)
        BigDecimal igdbRating,
        @Schema(description = "개인화 추천 점수", example = "12.7")
        BigDecimal recommendationScore,
        @Schema(description = "장르 목록")
        List<GameDetailResponse.GenreResponse> genres
) {
}
