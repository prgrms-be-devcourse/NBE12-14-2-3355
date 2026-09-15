package com.gamelog.nbe121423355.domain.usergame.entity;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private boolean inLibrary=true;

//    public UserGame(User user, Game game) {
//        this.user = Objects.requireNonNull(user, "user");
//        this.game = Objects.requireNonNull(game, "game");
//    }

    public UserGame(
            User user,
            Game game,
            PlayStatus playStatus,
            boolean playing,
            boolean backlog,
            boolean wishlist,
            boolean liked,
            Platform platform,
            BigDecimal playTimeHours,
            BigDecimal finishTimeHours,
            BigDecimal masterTimeHours,
            LocalDate startedAt,
            LocalDate completedAt,
            LocalDateTime lastPlayedAt
    ) {
        this.user = Objects.requireNonNull(user);
        this.game = Objects.requireNonNull(game);
        this.playStatus = playStatus;
        this.playing = playing;
        this.backlog = backlog;
        this.wishlist = wishlist;
        this.liked = liked;
        this.platform = platform;
        this.playTimeHours = playTimeHours;
        this.finishTimeHours = finishTimeHours;
        this.masterTimeHours = masterTimeHours;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.lastPlayedAt = lastPlayedAt;
    }

    public void updatePlayRecord(
            PlayStatus playStatus,
            boolean playing,
            boolean backlog,
            boolean wishlist,
            boolean liked,
            Platform platform,
            BigDecimal playTimeHours,
            BigDecimal finishTimeHours,
            BigDecimal masterTimeHours,
            LocalDate startedAt,
            LocalDate completedAt,
            LocalDateTime lastPlayedAt
    ) {
        this.playStatus = playStatus;
        this.playing = playing;
        this.backlog = backlog;
        this.wishlist = wishlist;
        this.liked = liked;
        this.platform = platform;
        this.playTimeHours = playTimeHours;
        this.finishTimeHours = finishTimeHours;
        this.masterTimeHours = masterTimeHours;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.lastPlayedAt = lastPlayedAt;
    }

    public void changePlayStatus(PlayStatus playStatus) {
        this.playStatus = playStatus;
    }

    public void clearPlayStatus() {
        this.playStatus = null;
    }

    public void changePlaying(boolean playing) {
        this.playing = playing;
    }

    public void changeBacklog(boolean backlog) {
        this.backlog = backlog;
    }

    public void changeWishlist(boolean wishlist) {
        this.wishlist = wishlist;
    }

    public void changeLiked(boolean liked) {
        this.liked = liked;
    }

    public void activateLibrary() {
        this.inLibrary = true;
    }

    public void deactivateLibrary() {
        this.inLibrary = false;
    }
}
