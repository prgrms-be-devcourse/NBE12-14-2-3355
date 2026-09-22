package com.gamelog.nbe121423355.domain.game.repository.projection;

public interface PopularGameProjection {

    Long getGameId();

    String getTitle();

    String getCoverImageUrl();

    long getLikeCount();
}