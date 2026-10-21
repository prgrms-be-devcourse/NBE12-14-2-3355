package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCalculation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RelatedGameCalculationRepository extends JpaRepository<RelatedGameCalculation, Long> {

    boolean existsByGameIdAndLastSuccessAtIsNotNull(Long gameId);

    // 최초 계산 미완료 대상을 ID 순서로 나누어 조회한다.
    @Query("""
            SELECT calculation.gameId FROM RelatedGameCalculation calculation
            WHERE calculation.lastSuccessAt IS NULL AND calculation.gameId > :afterGameId
            ORDER BY calculation.gameId
            """)
    List<Long> findUnreadyGameIdsAfter(@Param("afterGameId") Long afterGameId, Pageable pageable);
}
