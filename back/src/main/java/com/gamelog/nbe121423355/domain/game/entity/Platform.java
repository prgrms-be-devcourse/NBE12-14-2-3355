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

    @Column(name = "igdb_platforms_id")
    private Long igdbPlatformsId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
}
