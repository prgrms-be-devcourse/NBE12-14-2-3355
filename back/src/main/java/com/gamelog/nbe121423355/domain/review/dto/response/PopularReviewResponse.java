package com.gamelog.nbe121423355.domain.review.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PopularReviewResponse(
        Long reviewId,
        Long userId,
        String nickname,
        String profileImageUrl,
        Long gameId,
        String gameTitle,
        String gameCoverImageUrl,
        BigDecimal rating,
        String content,
        boolean spoiler,
        long likeCount,
        LocalDateTime createdDate
) {
}