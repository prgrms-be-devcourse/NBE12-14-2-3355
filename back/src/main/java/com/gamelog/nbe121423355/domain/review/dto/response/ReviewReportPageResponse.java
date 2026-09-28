package com.gamelog.nbe121423355.domain.review.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReviewReportPageResponse(
        List<ReviewReportGroupResponse> reports,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ReviewReportPageResponse from(
            Page<Long> reviewIdPage,
            List<ReviewReportGroupResponse> reports
    ) {
        return new ReviewReportPageResponse(
                reports,
                reviewIdPage.getNumber(),
                reviewIdPage.getSize(),
                reviewIdPage.getTotalElements(),
                reviewIdPage.getTotalPages(),
                reviewIdPage.hasNext()
        );
    }
}
