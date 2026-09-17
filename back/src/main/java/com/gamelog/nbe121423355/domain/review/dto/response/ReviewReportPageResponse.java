package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import org.springframework.data.domain.Page;

import java.util.List;

public record ReviewReportPageResponse(
        List<ReviewReportResponse> reports,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ReviewReportPageResponse from(Page<ReviewReport> reportPage) {
        List<ReviewReportResponse> reports = reportPage.getContent()
                .stream()
                .map(ReviewReportResponse::from)
                .toList();

        return new ReviewReportPageResponse(
                reports,
                reportPage.getNumber(),
                reportPage.getSize(),
                reportPage.getTotalElements(),
                reportPage.getTotalPages(),
                reportPage.hasNext()
        );
    }
}
