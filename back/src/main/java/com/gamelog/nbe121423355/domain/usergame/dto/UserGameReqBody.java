package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserGameReqBody(
        PlayStatus playStatus,
        boolean isPlaying,
        boolean isBacklog,
        boolean isWishlist,
        boolean isLiked,
        Long platformId,

        @PositiveOrZero
        BigDecimal playTimeHours,

        @PositiveOrZero
        BigDecimal finishTimeHours,

        @PositiveOrZero
        BigDecimal masterTimeHours,

        LocalDate startedAt,
        LocalDate completedAt,
        LocalDateTime lastPlayedAt
){}