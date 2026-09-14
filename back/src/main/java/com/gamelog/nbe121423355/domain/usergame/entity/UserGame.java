package com.gamelog.nbe121423355.domain.usergame.entity;

import com.gamelog.nbe121423355.domain.game.entity.GamePlatform;
import com.gamelog.nbe121423355.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.graalvm.nativeimage.Platform;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_games", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "game_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGame extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_status")
    private PlayStatus playStatus;

    @Column(name = "is_playing", nullable = false)
    private boolean playing;

    @Column(name = "is_backlog", nullable = false)
    private boolean backlog;

    @Column(name = "is_wishlist", nullable = false)
    private boolean wishlist;

    @Column(name = "is_liked", nullable = false)
    private boolean liked;

    // 게임의 지원 플랫폼 제약은 통합 SQL의 복합 FK로 보장한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private Platform platform;

    @Column(name = "play_time_hours", precision = 8, scale = 2)
    private BigDecimal playTimeHours;

    @Column(name = "finish_time_hours", precision = 8, scale = 2)
    private BigDecimal finishTimeHours;

    @Column(name = "master_time_hours", precision = 8, scale = 2)
    private BigDecimal masterTimeHours;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    public UserGame(User user, Game game) {
        this.user = Objects.requireNonNull(user, "user");
        this.game = Objects.requireNonNull(game, "game");
    }
}
