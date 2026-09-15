package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.Review;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long userGameId,
        BigDecimal rating,
        String content,
        boolean spoiler,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserGame().getId(),
                review.getRating(),
                review.getContent(),
                review.isSpoiler(),
                review.getCreatedDate(),
                review.getLastModifiedDate()
        );
    }
}
