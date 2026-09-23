package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserGameReqBody(
        @Schema(description = "플레이 완료 세부 상태", example = "COMPLETED", nullable = true)
        PlayStatus playStatus,
        @Schema(description = "현재 플레이 중 여부", example = "false")
        boolean isPlaying,
        @Schema(description = "플레이 예정 여부", example = "false")
        boolean isBacklog,
        @Schema(description = "위시리스트 여부", example = "false")
        boolean isWishlist,
        @Schema(description = "좋아하는 게임 여부", example = "true")
        boolean isLiked,
        @Schema(description = "선택한 플랫폼 ID", example = "1", nullable = true)
        Long platformId,

        @Schema(description = "실제 플레이 시간", example = "42.5", minimum = "0", nullable = true)
        @PositiveOrZero
        BigDecimal playTimeHours,

        @Schema(description = "메인 목표 완료 시간", example = "35.0", minimum = "0", nullable = true)
        @PositiveOrZero
        BigDecimal finishTimeHours,

        @Schema(description = "완전 공략 시간", example = "70.0", minimum = "0", nullable = true)
        @PositiveOrZero
        BigDecimal masterTimeHours,

        @Schema(description = "플레이 시작일", example = "2026-09-01", nullable = true)
        LocalDate startedAt,
        @Schema(description = "플레이 완료일", example = "2026-09-20", nullable = true)
        LocalDate completedAt,
        @Schema(description = "마지막 플레이 일시", example = "2026-09-20T21:30:00", nullable = true)
        LocalDateTime lastPlayedAt
){}
