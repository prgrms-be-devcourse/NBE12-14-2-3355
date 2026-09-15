package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.GamePlatform;
import com.gamelog.nbe121423355.domain.game.entity.GamePlatformId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlatformRepository
        extends JpaRepository<GamePlatform, GamePlatformId> {
}
