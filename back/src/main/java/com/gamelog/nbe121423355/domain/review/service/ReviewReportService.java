package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportCreateRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportStatusUpdateRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewReportGroupResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewReportPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewReportResponse;
import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import com.gamelog.nbe121423355.domain.review.repository.ReviewReportRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewReportResponse createReport(
            Long reporterId,
            Long reviewId,
            ReviewReportCreateRequest request
    ) {
        Review review = getReview(reviewId);
        User reporter = getUser(reporterId);

        // 본인이 작성한 리뷰는 신고할 수 없음.
        Long reviewWriterId = review.getUserGame().getUser().getId();
        if (Objects.equals(reviewWriterId, reporterId)) {
            throw new ServiceException("400-3", "본인이 작성한 리뷰는 신고할 수 없습니다.");
        }

        // 같은 사용자가 같은 리뷰를 여러 번 신고할 수 없음.
        if (reviewReportRepository.existsByReview_IdAndReporter_Id(reviewId, reporterId)) {
            throw new ServiceException("409-1", "이미 신고한 리뷰입니다.");
        }

        ReviewReport report = new ReviewReport(review, reporter, request.reason());
        return ReviewReportResponse.from(reviewReportRepository.save(report));
    }

    public ReviewReportPageResponse getReports(ReportStatus status, Pageable pageable) {
        Pageable groupPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Long> reviewIdPage = reviewReportRepository.findReviewIdsGroupedByReview(
                status,
                groupPageable
        );

        if (reviewIdPage.isEmpty()) {
            return ReviewReportPageResponse.from(reviewIdPage, List.of());
        }

        List<ReviewReport> reports = reviewReportRepository.findAllByReviewIdsAndStatus(
                reviewIdPage.getContent(),
                status
        );
        Map<Long, List<ReviewReport>> reportsByReviewId = reports.stream()
                .collect(Collectors.groupingBy(
                        report -> report.getReview().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ReviewReportGroupResponse> groups = reviewIdPage.getContent().stream()
                .map(reportsByReviewId::get)
                .filter(Objects::nonNull)
                .map(ReviewReportGroupResponse::from)
                .toList();

        return ReviewReportPageResponse.from(reviewIdPage, groups);
    }

    @Transactional
    public ReviewReportResponse updateStatus(
            Long reportId,
            ReviewReportStatusUpdateRequest request
    ) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ServiceException("404-5", "리뷰 신고를 찾을 수 없습니다."));

        if (request.status() == ReportStatus.PENDING) {
            throw new ServiceException("400-5", "신고 처리 결과는 승인 또는 반려여야 합니다.");
        }

        List<ReviewReport> pendingReports = reviewReportRepository.findByReview_IdAndStatus(
                report.getReview().getId(),
                ReportStatus.PENDING
        );
        if (pendingReports.isEmpty()) {
            throw new ServiceException("409-4", "이미 처리된 리뷰 신고입니다.");
        }

        if (request.status() == ReportStatus.APPROVED) {
            report.getReview().hideByAdmin();
        }

        pendingReports.forEach(pendingReport -> pendingReport.changeStatus(request.status()));
        return ReviewReportResponse.from(pendingReports.get(0));
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findActiveById(reviewId)
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-4", "사용자를 찾을 수 없습니다."));
    }
}
