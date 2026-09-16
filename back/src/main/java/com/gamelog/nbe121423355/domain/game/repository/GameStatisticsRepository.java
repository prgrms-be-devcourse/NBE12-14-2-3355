package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    // 게임 ID를 기준으로 평균 평점과 리뷰 수를 집계
    @Query("""
            SELECT
                COALESCE(AVG(review.rating), 0.0) AS averageRating,
                COUNT(review.id) AS reviewCount
            FROM Review review
            WHERE review.userGame.game.id = :gameId
            """)
    GameRatingStatisticsProjection findRatingStatisticsByGameId(
            @Param("gameId") Long gameId
    );

    // 게임 ID를 기준으로 평점별 리뷰 수를 집계
    @Query("""
            SELECT
                review.rating AS rating,
                COUNT(review.id) AS count
            FROM Review review
            WHERE review.userGame.game.id = :gameId
            GROUP BY review.rating
            ORDER BY review.rating
            """)
    List<GameRatingDistributionProjection> findRatingDistributionByGameId(
            @Param("gameId") Long gameId
    );

    // 게임 ID를 기준으로 좋아요를 누른 사용자 수를 집계
    @Query("""
            SELECT COUNT(userGame.id)
            FROM UserGame userGame
            WHERE userGame.game.id = :gameId
                AND userGame.liked = true
            """)
    long countLikesByGameId(
            @Param("gameId") Long gameId
    );

}
