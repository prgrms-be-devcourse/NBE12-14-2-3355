package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GamePlatform;
import com.gamelog.nbe121423355.domain.game.entity.GamePlatformId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GamePlatformRepository extends JpaRepository<GamePlatform, GamePlatformId> {

    // 게임 ID로 플랫폼 정보를 함께 조회하고 플랫폼 이름순으로 정렬
    @Query("""
            SELECT gamePlatform
            FROM GamePlatform gamePlatform
            JOIN FETCH gamePlatform.platform
            WHERE gamePlatform.game.id = :gameId
            ORDER BY gamePlatform.platform.name
            """)
    List<GamePlatform> findAllWithPlatformByGameId(
            @Param("gameId") Long gameId
    );
}
