package com.gamelog.nbe121423355.domain.usergame.dto;

import java.util.List;

public record UserProfileResponse(
        ProfileStatsDto stats,
        List<UserGameScatterDto> scatterData,
        UserGameTasteDto tasteResponse,
        List<UserGameGenreDistributionResponse> genreDistribution
) {
}
