package com.gamelog.nbe121423355.domain.game.repository.projection;

// GameStatisticsRepository 집계 쿼리의 alias를 동일한 이름의 getter에 매핑
public interface GameStatusStatisticsProjection {

    Long getPlayedCount();

    Long getPlayingCount();

    Long getBacklogCount();

    Long getWishlistCount();
}
