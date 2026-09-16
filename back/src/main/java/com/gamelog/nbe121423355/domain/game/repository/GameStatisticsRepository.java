package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// 통계 조회 전용 -> JpaRepository 대신 Repository 상속
public interface GameStatisticsRepository extends Repository<UserGame, Long> {

    // 게임 ID를 기준으로 Played, Playing, Backlog, Wishlist 인원수를 한 번에 집계
    @Query("""
            SELECT
                COALESCE(SUM(
                    CASE WHEN userGame.playStatus IS NOT NULL
                    THEN 1L ELSE 0L END
                ), 0L) AS playedCount,
                COALESCE(SUM(
                    CASE WHEN userGame.playing = true
                    THEN 1L ELSE 0L END
                ), 0L) AS playingCount,
                COALESCE(SUM(
                    CASE WHEN userGame.backlog = true
                    THEN 1L ELSE 0L END
                ), 0L) AS backlogCount,
                COALESCE(SUM(
                    CASE WHEN userGame.wishlist = true
                    THEN 1L ELSE 0L END
                ), 0L) AS wishlistCount
            FROM UserGame userGame
            WHERE userGame.game.id = :gameId
            """)
    GameStatusStatisticsProjection findStatusStatisticsByGameId(
            @Param("gameId") Long gameId
    );

}
