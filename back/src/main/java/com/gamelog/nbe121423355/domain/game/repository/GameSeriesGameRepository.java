package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GameSeriesGame;
import com.gamelog.nbe121423355.domain.game.entity.GameSeriesGameId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameSeriesGameRepository extends JpaRepository<GameSeriesGame, GameSeriesGameId> {

    // 게임 ID로 시리즈 정보를 함께 조회하고 시리즈 이름순으로 정렬
    @Query("""
            SELECT seriesGame
            FROM GameSeriesGame seriesGame
            JOIN FETCH seriesGame.series
            WHERE seriesGame.game.id = :gameId
            ORDER BY seriesGame.series.name
            """)
    List<GameSeriesGame> findAllWithSeriesByGameId(
            @Param("gameId") Long gameId
    );
}
