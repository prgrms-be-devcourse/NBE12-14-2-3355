package com.gamelog.nbe121423355.domain.game.repository.projection;

// 평균 플레이타임과 플레이타임 기록 사용자 수 집계 결과를 매핑
public interface GamePlayTimeStatisticsProjection {

    Double getAveragePlayTimeHours();

    Long getPlayTimeUserCount();

}
