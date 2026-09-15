package com.gamelog.nbe121423355.domain.game.dto;

import com.gamelog.nbe121423355.domain.game.entity.Game;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GameListResponse(
        Long id,
        String title,
        String coverImageUrl,
        LocalDate releaseDate,
        BigDecimal igdbRating
) {
    public GameListResponse(Game game){
        this(
                game.getId(),
                game.getTitle(),
                game.getCoverImageUrl(),
                game.getReleaseDate(),
                game.getIgdbRating()
        );
    }
}
