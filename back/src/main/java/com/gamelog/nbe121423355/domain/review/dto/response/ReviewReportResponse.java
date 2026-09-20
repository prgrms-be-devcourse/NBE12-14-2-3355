package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewReportResponse(
        Long reportId,
        Long reviewId,
        Long reporterId,
        Long reviewWriterId,
        String reason,
        BigDecimal reviewRating,
        String reviewContent,
        boolean reviewSpoiler,
        ReviewStatus reviewStatus,
        ReportStatus status,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

    public static ReviewReportResponse from(ReviewReport report) {
        return new ReviewReportResponse(
                report.getId(),
                report.getReview().getId(),
                report.getReporter().getId(),
                report.getReview().getUserGame().getUser().getId(),
                report.getReason(),
                report.getReviewRatingSnapshot() != null
                        ? report.getReviewRatingSnapshot()
                        : report.getReview().getRating(),
                report.getReviewContentSnapshot() != null
                        ? report.getReviewContentSnapshot()
                        : report.getReview().getContent(),
                report.getReviewSpoilerSnapshot() != null
                        ? report.getReviewSpoilerSnapshot()
                        : report.getReview().isSpoiler(),
                report.getReview().getStatus() == null
                        ? ReviewStatus.ACTIVE
                        : report.getReview().getStatus(),
                report.getStatus(),
                report.getCreatedDate(),
                report.getLastModifiedDate()
        );
    }
}
