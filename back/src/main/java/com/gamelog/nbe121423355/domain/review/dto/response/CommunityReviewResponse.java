package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.repository.CommunityReviewProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommunityReviewResponse(
        Long reviewId, Long userId, String nickname, String profileImageUrl,
        Long gameId, String gameTitle, String gameCoverImageUrl, BigDecimal rating,
        String content, boolean spoiler, long likeCount, LocalDateTime createdDate, boolean liked
) {
    public static CommunityReviewResponse from(CommunityReviewProjection r) {
        return new CommunityReviewResponse(r.getReviewId(), r.getUserId(), r.getNickname(),
                r.getProfileImageUrl(), r.getGameId(), r.getGameTitle(), r.getGameCoverImageUrl(),
                r.getRating(), r.getContent(), r.getSpoiler(), r.getLikeCount(), r.getCreatedDate(), r.getLiked());
    }
}
