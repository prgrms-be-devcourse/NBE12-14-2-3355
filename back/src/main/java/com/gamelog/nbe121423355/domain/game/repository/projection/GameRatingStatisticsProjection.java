package com.gamelog.nbe121423355.domain.game.repository.projection;

// 평균 평점, 평가 수와 내용이 있는 리뷰 수 집계 결과를 매핑
public interface GameRatingStatisticsProjection {

    Double getAverageRating();

    Long getRatingCount();

    Long getReviewCount();
}
