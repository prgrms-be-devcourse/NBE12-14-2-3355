package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long userGameId,
        Long gameId,
        String gameTitle,
        String gameCoverImageUrl,
        LocalDate gameReleaseDate,
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
        long likeCount,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

    public static ReviewResponse from(Review review) {
        return from(review, 0L);
    }

    public static ReviewResponse from(Review review, long likeCount) {
        return new ReviewResponse(
                review.getId(),
                review.getUserGame().getId(),
                review.getUserGame().getGame().getId(),
                review.getUserGame().getGame().getTitle(),
                review.getUserGame().getGame().getCoverImageUrl(),
                review.getUserGame().getGame().getReleaseDate(),
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
                likeCount,
                review.getCreatedDate(),
                review.getLastModifiedDate()
        );
    }
}
