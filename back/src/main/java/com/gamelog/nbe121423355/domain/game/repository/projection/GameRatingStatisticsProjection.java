package com.gamelog.nbe121423355.domain.game.repository.projection;

// 평균 평점과 리뷰 수 집계 결과를 매핑
public interface GameRatingStatisticsProjection {

    Double getAverageRating();

    Long getReviewCount();
}
