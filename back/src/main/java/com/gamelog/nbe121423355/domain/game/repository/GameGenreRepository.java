package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GameGenre;
import com.gamelog.nbe121423355.domain.game.entity.GameGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameGenreRepository
        extends JpaRepository<GameGenre, GameGenreId> {
}
