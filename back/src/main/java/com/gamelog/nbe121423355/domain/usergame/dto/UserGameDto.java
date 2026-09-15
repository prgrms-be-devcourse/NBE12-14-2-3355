package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserGameDto(
        User user,
        Game game,
        Platform platform,
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
        LocalDateTime lastPlayedAt
) {
    public UserGameDto(UserGame userGame){
        this(
                userGame.getUser(),
                userGame.getGame(),
                userGame.getPlatform(),
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
                userGame.getLastPlayedAt()
        );
    }
}
