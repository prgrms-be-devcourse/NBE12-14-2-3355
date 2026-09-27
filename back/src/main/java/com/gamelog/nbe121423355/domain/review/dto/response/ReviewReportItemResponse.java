package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;

import java.time.LocalDateTime;

public record ReviewReportItemResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        String reason,
        ReportStatus status,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

    public static ReviewReportItemResponse from(ReviewReport report) {
        return new ReviewReportItemResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedDate(),
                report.getLastModifiedDate()
        );
    }
}
