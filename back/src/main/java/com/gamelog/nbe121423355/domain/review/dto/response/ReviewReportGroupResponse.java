package com.gamelog.nbe121423355.domain.review.dto.response;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record ReviewReportGroupResponse(
        Long reviewId,
        Long reviewWriterId,
        String reviewWriterNickname,
        BigDecimal reviewRating,
        String reviewContent,
        boolean reviewSpoiler,
        ReviewStatus reviewStatus,
        ReportStatus status,
        int reportCount,
        int pendingReportCount,
        Long representativeReportId,
        LocalDateTime latestReportedAt,
        List<ReviewReportItemResponse> reports
) {

    public static ReviewReportGroupResponse from(List<ReviewReport> source) {
        if (source.isEmpty()) {
            throw new IllegalArgumentException("신고 묶음은 비어 있을 수 없습니다.");
        }

        List<ReviewReport> sorted = source.stream()
                .sorted(Comparator.comparing(
                        ReviewReport::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        ReviewReport representative = sorted.stream()
                .filter(report -> report.getStatus() == ReportStatus.PENDING)
                .findFirst()
                .orElse(sorted.get(0));

        ReportStatus groupStatus = sorted.stream()
                .anyMatch(report -> report.getStatus() == ReportStatus.PENDING)
                ? ReportStatus.PENDING
                : sorted.stream().anyMatch(report -> report.getStatus() == ReportStatus.APPROVED)
                ? ReportStatus.APPROVED
                : ReportStatus.REJECTED;

        return new ReviewReportGroupResponse(
                representative.getReview().getId(),
                representative.getReview().getUserGame().getUser().getId(),
                representative.getReview().getUserGame().getUser().getNickname(),
                representative.getReviewRatingSnapshot() != null
                        ? representative.getReviewRatingSnapshot()
                        : representative.getReview().getRating(),
                representative.getReviewContentSnapshot() != null
                        ? representative.getReviewContentSnapshot()
                        : representative.getReview().getContent(),
                representative.getReviewSpoilerSnapshot() != null
                        ? representative.getReviewSpoilerSnapshot()
                        : representative.getReview().isSpoiler(),
                representative.getReview().getStatus() == null
                        ? ReviewStatus.ACTIVE
                        : representative.getReview().getStatus(),
                groupStatus,
                sorted.size(),
                (int) sorted.stream()
                        .filter(report -> report.getStatus() == ReportStatus.PENDING)
                        .count(),
                representative.getId(),
                representative.getCreatedDate(),
                sorted.stream().map(ReviewReportItemResponse::from).toList()
        );
    }
}
