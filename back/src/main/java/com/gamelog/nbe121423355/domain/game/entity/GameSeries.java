package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_series")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    @Column(nullable = false, length = 255)
    private String name;

    public static GameSeries createFromIgdb(Long igdbId, String name) {
        GameSeries series = new GameSeries();
        series.igdbId = igdbId;
        series.name = name;
        return series;
    }
}
