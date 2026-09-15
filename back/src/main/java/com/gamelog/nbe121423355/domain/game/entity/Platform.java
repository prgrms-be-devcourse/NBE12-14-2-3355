package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platforms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Platform
    @Column(name = "igdb_platforms_id", unique = true)
    private Long igdbPlatformsId;

    // Platform의 name
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    public static Platform createFromIgdb(Long igdbId, String name) {
        Platform platform = new Platform();
        platform.igdbPlatformsId = igdbId;
        platform.name = name;
        return platform;
    }
}
