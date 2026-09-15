package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GameSeriesGame;
import com.gamelog.nbe121423355.domain.game.entity.GameSeriesGameId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSeriesGameRepository
        extends JpaRepository<GameSeriesGame, GameSeriesGameId> {
}
