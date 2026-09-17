package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewResponse> reviews,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ReviewPageResponse from(Page<Review> reviewPage) {
        List<ReviewResponse> reviews = reviewPage.getContent()
                .stream()
                .map(ReviewResponse::from)
                .toList();

        return new ReviewPageResponse(
                reviews,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.hasNext()
        );
    }
}
