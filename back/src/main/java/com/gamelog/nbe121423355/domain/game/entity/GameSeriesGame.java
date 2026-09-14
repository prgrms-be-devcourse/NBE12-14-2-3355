package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "game_series_games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSeriesGame {

    @EmbeddedId
    private GameSeriesGameId id;

    @MapsId("seriesId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private GameSeries series;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // ID가 부여된 부모 엔티티를 연결한다.
    public GameSeriesGame(GameSeries series, Game game) {
        this.series = Objects.requireNonNull(series, "series");
        this.game = Objects.requireNonNull(game, "game");
        this.id = new GameSeriesGameId(series.getId(), game.getId());
    }
}
