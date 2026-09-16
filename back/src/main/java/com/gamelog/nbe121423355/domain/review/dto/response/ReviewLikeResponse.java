package com.gamelog.nbe121423355.domain.review.dto.response;

public record ReviewLikeResponse(
        Long reviewId,
        long likeCount,
        boolean liked
) {
}
