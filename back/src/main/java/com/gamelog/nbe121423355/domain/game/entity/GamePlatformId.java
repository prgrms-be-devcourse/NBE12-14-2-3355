package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GamePlatformId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;
}