package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;

public record DetailedReviewResponse(
        UserGameDto userGame,
        ReviewResponse review
) {
}
