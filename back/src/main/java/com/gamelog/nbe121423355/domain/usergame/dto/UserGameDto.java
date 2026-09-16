package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserGameDto(
        Long id,
        Long userId,
        Long gameId,
        Long platformId,
        PlayStatus playStatus,
        boolean playing,
        boolean backlog,
        boolean wishlist,
        boolean liked,
        BigDecimal playTimeHours,
        BigDecimal finishTimeHours,
        BigDecimal masterTimeHours,
        LocalDate startedAt,
        LocalDate completedAt,
        LocalDateTime lastPlayedAt,
        boolean inLibrary
) {
    public UserGameDto(UserGame userGame){
        this(
                userGame.getId(),
                userGame.getUser().getId(),
                userGame.getGame().getId(),
                userGame.getPlatform().getId(),
                userGame.getPlayStatus(),
                userGame.isPlaying(),
                userGame.isBacklog(),
                userGame.isWishlist(),
                userGame.isLiked(),
                userGame.getPlayTimeHours(),
                userGame.getFinishTimeHours(),
                userGame.getMasterTimeHours(),
                userGame.getStartedAt(),
                userGame.getCompletedAt(),
                userGame.getLastPlayedAt(),
                userGame.isInLibrary()
        );
    }
}
