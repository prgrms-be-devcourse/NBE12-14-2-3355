package com.gamelog.nbe121423355.domain.game.dto;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "게임 목록 항목")
public record GameListResponse(
        @Schema(description = "GameLog 게임 ID", example = "6")
        Long id,
        @Schema(description = "게임명", example = "Baldur's Gate II: Shadows of Amn")
        String title,
        @Schema(description = "커버 이미지 URL", nullable = true)
        String coverImageUrl,
        @Schema(description = "출시일", example = "2000-09-21", nullable = true)
        LocalDate releaseDate,
        @Schema(description = "IGDB 평점", example = "89.5", nullable = true)
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
