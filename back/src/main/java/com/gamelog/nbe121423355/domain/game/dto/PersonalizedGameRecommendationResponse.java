package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

// 사용자 맞춤 추천 게임의 기본 정보, 추천 점수, 장르를 반환
public record PersonalizedGameRecommendationResponse(
        Long id,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        BigDecimal recommendationScore,
        List<GameDetailResponse.GenreResponse> genres
) {
}