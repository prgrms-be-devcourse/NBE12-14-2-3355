package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "인기 게임 정보")
public record PopularGameResponse(
        @Schema(description = "GameLog 게임 ID", example = "6")
        Long gameId,
        @Schema(description = "게임명", example = "Baldur's Gate II: Shadows of Amn")
        String title,
        @Schema(description = "커버 이미지 URL", nullable = true)
        String coverImageUrl,
        @Schema(description = "좋아하는 게임 등록 수", example = "87")
        long likeCount,
        @Schema(description = "장르 목록")
        List<GenreResponse> genres
) {

    @Schema(description = "인기 게임 장르")
    public record GenreResponse(
            @Schema(description = "장르 ID", example = "12")
            Long id,
            @Schema(description = "장르명", example = "Role-playing (RPG)")
            String name
    ) {
    }
}
