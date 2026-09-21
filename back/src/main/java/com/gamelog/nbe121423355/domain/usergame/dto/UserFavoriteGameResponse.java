package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGame;

public record UserFavoriteGameResponse(
        Long gameId,
        String title,
        String coverImageUrl,
        int displayOrder
) {
    public UserFavoriteGameResponse(UserFavoriteGame favoriteGame) {
        this(
                favoriteGame.getGame().getId(),
                favoriteGame.getGame().getTitle(),
                favoriteGame.getGame().getCoverImageUrl(),
                favoriteGame.getDisplayOrder()
        );
    }
}