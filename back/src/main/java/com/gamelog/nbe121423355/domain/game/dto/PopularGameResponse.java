package com.gamelog.nbe121423355.domain.game.dto;

import java.util.List;

public record PopularGameResponse(
        Long gameId,
        String title,
        String coverImageUrl,
        long likeCount,
        List<GenreResponse> genres
) {

    public record GenreResponse(
            Long id,
            String name
    ) {
    }
}