package com.gamelog.nbe121423355.domain.game.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

// 최종 점수 계산에 필요한 추천 후보의 게임 정보, 사용자 평균 평점, 장르를 매핑
public record PersonalizedRecommendationCandidateProjection(
        Long gameId,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        Double averageRating,
        LocalDate releaseDate,
        Long genreId,
        String genreName
) {
}