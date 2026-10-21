package com.gamelog.nbe121423355.domain.review.repository;

public interface CommunityReviewProjection extends PopularReviewProjection {
    boolean getLiked();
    com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus getPlayStatus();
    boolean getPlaying();
    boolean getBacklog();
    boolean getWishlist();
}
