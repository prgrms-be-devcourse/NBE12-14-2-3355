package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GameSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSeriesRepository
        extends JpaRepository<GameSeries, Long> {

    Optional<GameSeries> findByIgdbId(Long igdbId);
}