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

    @Column(name = "igdb_id")
    private Long igdbId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

}
