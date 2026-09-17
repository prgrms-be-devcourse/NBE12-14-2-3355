package com.gamelog.nbe121423355.domain.game.repository;

import java.math.BigDecimal;

// 평점별 리뷰 수 집계 결과를 매핑
public interface GameRatingDistributionProjection {

    BigDecimal getRating();

    Long getCount();
}
