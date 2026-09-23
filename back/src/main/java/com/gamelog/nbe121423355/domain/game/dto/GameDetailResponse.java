package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "게임 상세 정보")
public record GameDetailResponse(
        @Schema(description = "GameLog 게임 ID", example = "6")
        Long id,
        @Schema(description = "IGDB 게임 ID", example = "3030")
        Long igdbId,
        @Schema(description = "게임명", example = "Baldur's Gate II: Shadows of Amn")
        String title,
        @Schema(description = "커버 이미지 URL", nullable = true)
        String coverImageUrl,
        @Schema(description = "개발사", example = "BioWare", nullable = true)
        String developer,
        @Schema(description = "출시일", example = "2000-09-21", nullable = true)
        LocalDate releaseDate,
        @Schema(description = "게임 설명", nullable = true)
        String description,
        @Schema(description = "IGDB 평점", example = "89.5", nullable = true)
        BigDecimal igdbRating,
        @Schema(description = "장르 목록")
        List<GenreResponse> genres,
        @Schema(description = "지원 플랫폼 목록")
        List<PlatformResponse> platforms,
        @Schema(description = "시리즈 목록")
        List<SeriesResponse> series,
        @Schema(description = "GameLog 사용자 기록 통계")
        GameStatisticsResponse statistics
) {

    @Schema(description = "게임 장르")
    public record GenreResponse(
            @Schema(description = "장르 ID", example = "12")
            Long id,
            @Schema(description = "장르명", example = "Role-playing (RPG)")
            String name
    ) {
    }

    @Schema(description = "게임 플랫폼")
    public record PlatformResponse(
            @Schema(description = "플랫폼 ID", example = "6")
            Long id,
            @Schema(description = "플랫폼명", example = "PC (Microsoft Windows)")
            String name
    ) {
    }

    @Schema(description = "게임 시리즈")
    public record SeriesResponse(
            @Schema(description = "시리즈 ID", example = "1")
            Long id,
            @Schema(description = "시리즈명", example = "Baldur's Gate")
            String name
    ) {
    }

}
