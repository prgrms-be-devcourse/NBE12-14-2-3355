package com.gamelog.nbe121423355.domain.usergame.dto;

import java.util.List;

public record UserProfileResponse(
        List<UserFavoriteGameResponse> favorite,
        ProfileStatsDto stats,
        List<UserGameScatterDto> scatterData,
        UserGameTasteDto tasteResponse,
        List<UserGameGenreDistributionResponse> genreDistribution,
        List<UserGameListResponse> recentGames,
        List<RecentReviewResponse> recentReviews
) {
}
