package com.gamelog.nbe121423355.domain.review.dto.response;

import java.util.List;

public record CommunityFeedResponse(
        List<CommunityReviewResponse> reviews, boolean hasNext, boolean hasFollowing
) {}
