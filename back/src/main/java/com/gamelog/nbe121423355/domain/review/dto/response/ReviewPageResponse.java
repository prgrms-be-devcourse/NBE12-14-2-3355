package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record ReviewPageResponse(
        List<ReviewResponse> reviews,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        long totalLikes
) {

    public static ReviewPageResponse from(
            Page<Review> reviewPage,
            Map<Long, Long> likeCounts,
            long totalLikes
    ) {
        List<ReviewResponse> reviews = reviewPage.getContent()
                .stream()
                .map(review -> ReviewResponse.from(
                        review,
                        likeCounts.getOrDefault(review.getId(), 0L)
                ))
                .toList();

        return new ReviewPageResponse(
                reviews,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.hasNext(),
                totalLikes
        );
    }
}
