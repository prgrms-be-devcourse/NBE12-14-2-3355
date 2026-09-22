package com.gamelog.nbe121423355.domain.review.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PopularReviewProjection {

    Long getReviewId();

    Long getUserId();

    String getNickname();

    String getProfileImageUrl();

    Long getGameId();

    String getGameTitle();

    String getGameCoverImageUrl();

    BigDecimal getRating();

    String getContent();

    boolean getSpoiler();

    long getLikeCount();

    LocalDateTime getCreatedDate();
}