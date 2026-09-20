package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long userGameId,
        Long userId,
        String nickname,
        String profileImageUrl,
        PlayStatus playStatus,
        boolean playing,
        boolean backlog,
        boolean wishlist,
        String platformName,
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
                review.getUserGame().getUser().getId(),
                review.getUserGame().getUser().getNickname(),
                review.getUserGame().getUser().getProfileImageUrl(),
                review.getUserGame().getPlayStatus(),
                review.getUserGame().isPlaying(),
                review.getUserGame().isBacklog(),
                review.getUserGame().isWishlist(),
                review.getUserGame().getPlatform() == null
                        ? null
                        : review.getUserGame().getPlatform().getName(),
                review.getRating(),
                review.getContent(),
                review.isSpoiler(),
                review.getCreatedDate(),
                review.getLastModifiedDate()
        );
    }
}
