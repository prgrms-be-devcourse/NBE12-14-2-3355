package com.gamelog.nbe121423355.domain.game.repository.projection;

import java.math.BigDecimal;

// 기준 게임별 연관 추천 상위 5개의 후보 정보와 순위를 매핑
public record PersonalizedRelatedGameProjection(
        Long sourceGameId,
        Long gameId,
        String title,
        String coverImageUrl,
        BigDecimal igdbRating,
        BigDecimal relatedScore,
        int relatedRank
) {
}
