package com.gamelog.nbe121423355.domain.usergame.dto;

import java.util.List;

public record UserProfileResponse(
        ProfileStatsResponse stats,
        List<UserGameScatterResponse> scatterData
) {
}
