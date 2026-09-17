package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportCreateRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportStatusUpdateRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
        if (status == null) {
            return ReviewReportPageResponse.from(reviewReportRepository.findAll(pageable));
        }

        return ReviewReportPageResponse.from(
                reviewReportRepository.findByStatus(status, pageable)
        );
    }

    @Transactional
    public ReviewReportResponse updateStatus(
            Long reportId,
            ReviewReportStatusUpdateRequest request
    ) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ServiceException("404-5", "리뷰 신고를 찾을 수 없습니다."));

        report.changeStatus(request.status());
        return ReviewReportResponse.from(report);
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-4", "사용자를 찾을 수 없습니다."));
    }
}
