package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;

import java.time.LocalDateTime;

public record ReviewReportResponse(
        Long reportId,
        Long reviewId,
        Long reporterId,
        String reason,
        ReportStatus status,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

    public static ReviewReportResponse from(ReviewReport report) {
        return new ReviewReportResponse(
                report.getId(),
                report.getReview().getId(),
                report.getReporter().getId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedDate(),
                report.getLastModifiedDate()
        );
    }
}
