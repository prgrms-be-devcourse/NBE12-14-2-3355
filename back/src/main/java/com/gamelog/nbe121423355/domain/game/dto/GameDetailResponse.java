package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GameDetailResponse(
        Long id,
        Long igdbId,
        String title,
        String coverImageUrl,
        String developer,
        LocalDate releaseDate,
        String description,
        BigDecimal igdbRating,
        List<GenreResponse> genres,
        List<PlatformResponse> platforms,
        List<SeriesResponse> series,
        GameStatisticsResponse statistics
) {

    public record GenreResponse(
            Long id,
            String name
    ) {
    }

    public record PlatformResponse(
            Long id,
            String name
    ) {
    }

    public record SeriesResponse(
            Long id,
            String name
    ) {
    }

}
