package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;

public record UserGameListResponse(
        Long gameId,
        String title,
        String coverImageUrl,
        PlayStatus playStatus,
        boolean playing,
        boolean backlog,
        boolean wishlist,
        boolean liked
) {
    public UserGameListResponse(UserGame userGame) {
        this(
                userGame.getGame().getId(),
                userGame.getGame().getTitle(),
                userGame.getGame().getCoverImageUrl(),
                userGame.getPlayStatus(),
                userGame.isPlaying(),
                userGame.isBacklog(),
                userGame.isWishlist(),
                userGame.isLiked()
        );
    }
}