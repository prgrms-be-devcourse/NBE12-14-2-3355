package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "related_game_calculations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelatedGameCalculation {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @Column(name = "game_id")
    private Long gameId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Game game;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_attempt_status", nullable = false, length = 20)
    private RelatedGameCalculationStatus lastAttemptStatus;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public RelatedGameCalculation(Game game) {
        this.game = Objects.requireNonNull(game, "game");
        Objects.requireNonNull(game.getId(), "저장된 게임이 필요합니다.");
        // 새 엔티티는 ID를 비워 persist로 저장하고, @MapsId가 게임 ID를 채우게 한다.
        this.lastAttemptStatus = RelatedGameCalculationStatus.PENDING;
    }

    public boolean isReady() {
        return lastSuccessAt != null;
    }

    public void start(LocalDateTime attemptedAt) {
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt");
        this.lastAttemptStatus = RelatedGameCalculationStatus.RUNNING;
        this.lastError = null;
    }

    public void succeed(LocalDateTime completedAt) {
        requireRunning();
        this.lastSuccessAt = Objects.requireNonNull(completedAt, "completedAt");
        this.lastAttemptStatus = RelatedGameCalculationStatus.SUCCESS;
        this.lastError = null;
    }

    // 실패 트랜잭션이 롤백되어 실행 시작 상태도 유실될 수 있으므로 실패 시각을 함께 받는다.
    public void fail(LocalDateTime attemptedAt, String error) {
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt");
        this.lastAttemptStatus = RelatedGameCalculationStatus.FAILED;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        // 기존 정상 결과의 준비 완료 여부는 실패와 독립적으로 유지한다.
    }

    private void requireRunning() {
        if (lastAttemptStatus != RelatedGameCalculationStatus.RUNNING) {
            throw new IllegalStateException("실행 중인 계산만 성공 처리할 수 있습니다.");
        }
    }
}
