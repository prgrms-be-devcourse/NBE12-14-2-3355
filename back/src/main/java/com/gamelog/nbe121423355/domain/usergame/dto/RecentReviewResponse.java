package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentReviewResponse (
        Long reviewId,
        Long gameId,
        String gameTitle,
        String gameCoverImageUrl,
        PlayStatus playStatus,
        String platform,
        BigDecimal rating,
        String content,
        LocalDateTime lastModifiedDate
){
    public RecentReviewResponse(Review review) {
        this(
                review.getId(),
                review.getUserGame().getGame().getId(),
                review.getUserGame().getGame().getTitle(),
                review.getUserGame().getGame().getCoverImageUrl(),
                review.getUserGame().getPlayStatus() != null
                        ? review.getUserGame().getPlayStatus()
                        : null,
                review.getUserGame().getPlatform() != null
                        ? review.getUserGame().getPlatform().getName()
                        : null,
                review.getRating(),
                review.getContent(),
                review.getLastModifiedDate()
        );
    }
}
