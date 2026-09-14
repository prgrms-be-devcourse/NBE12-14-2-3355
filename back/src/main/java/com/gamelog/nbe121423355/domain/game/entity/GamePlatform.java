package com.gamelog.nbe121423355.domain.game.entity;

import com.gamelog.nbe121423355.domain.game.entity.id.GamePlatformId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.graalvm.nativeimage.Platform;

import java.util.Objects;

@Entity
@Table(name = "game_platforms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GamePlatform {

    @EmbeddedId
    private GamePlatformId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @MapsId("platformId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    // ID가 부여된 부모 엔티티를 연결한다.
    public GamePlatform(Game game, Platform platform) {
        this.game = Objects.requireNonNull(game, "game");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.id = new GamePlatformId(game.getId(), platform.getId());
    }
}
