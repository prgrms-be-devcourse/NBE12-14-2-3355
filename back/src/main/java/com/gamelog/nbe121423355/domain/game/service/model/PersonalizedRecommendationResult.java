package com.gamelog.nbe121423355.domain.game.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 맞춤 추천의 최종 점수와 정렬 정보를 담는 서비스 내부 결과
public record PersonalizedRecommendationResult(
        Long gameId,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        Double averageRating,
        LocalDate releaseDate,
        BigDecimal recommendationScore,
        List<PersonalizedRecommendationGenre> genres
) {
}