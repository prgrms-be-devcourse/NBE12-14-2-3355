package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportCreateRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportStatusUpdateRequest;
import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import com.gamelog.nbe121423355.domain.review.repository.ReviewReportRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

    @Mock ReviewReportRepository reviewReportRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ReviewReportService reviewReportService;

    private User writer;
    private User reporter;
    private Review review;

    @BeforeEach
    void setUp() {
        writer = mock(User.class);
        reporter = mock(User.class);
        UserGame userGame = mock(UserGame.class);
        lenient().when(writer.getId()).thenReturn(1L);
        lenient().when(writer.getNickname()).thenReturn("리뷰작성자");
        lenient().when(reporter.getId()).thenReturn(2L);
        lenient().when(reporter.getNickname()).thenReturn("신고자");
        lenient().when(userGame.getUser()).thenReturn(writer);
        review = new Review(userGame, new BigDecimal("4.5"), "신고 대상 리뷰", false);
    }

    @DisplayName("다른 사용자의 활성 리뷰를 신고한다")
    @Test
    void createReport() {
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reporter));
        when(reviewReportRepository.existsByReview_IdAndReporter_Id(10L, 2L)).thenReturn(false);
        when(reviewReportRepository.save(org.mockito.ArgumentMatchers.any(ReviewReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = reviewReportService.createReport(2L, 10L, new ReviewReportCreateRequest("욕설 포함"));

        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.reason()).isEqualTo("욕설 포함");
        assertThat(response.reviewContent()).isEqualTo("신고 대상 리뷰");
        assertThat(response.reviewWriterNickname()).isEqualTo("리뷰작성자");
        assertThat(response.reporterNickname()).isEqualTo("신고자");
    }

    @DisplayName("신고를 승인하면 리뷰를 관리자 숨김 처리한다")
    @Test
    void approveReportHidesReview() {
        ReviewReport report = new ReviewReport(review, reporter, "부적절한 내용");
        when(reviewReportRepository.findById(20L)).thenReturn(Optional.of(report));

        var response = reviewReportService.updateStatus(
                20L,
                new ReviewReportStatusUpdateRequest(ReportStatus.APPROVED)
        );

        assertThat(response.status()).isEqualTo(ReportStatus.APPROVED);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN_BY_ADMIN);
        assertThat(review.getDeletedAt()).isNotNull();
    }

    @DisplayName("신고를 반려하면 리뷰 공개 상태를 유지한다")
    @Test
    void rejectReportKeepsReviewActive() {
        ReviewReport report = new ReviewReport(review, reporter, "문제 없음");
        when(reviewReportRepository.findById(20L)).thenReturn(Optional.of(report));

        var response = reviewReportService.updateStatus(
                20L,
                new ReviewReportStatusUpdateRequest(ReportStatus.REJECTED)
        );

        assertThat(response.status()).isEqualTo(ReportStatus.REJECTED);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @DisplayName("이미 처리된 신고는 다시 처리할 수 없다")
    @Test
    void rejectReprocessingReport() {
        ReviewReport report = new ReviewReport(review, reporter, "신고 사유");
        report.changeStatus(ReportStatus.REJECTED);
        when(reviewReportRepository.findById(20L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reviewReportService.updateStatus(
                20L,
                new ReviewReportStatusUpdateRequest(ReportStatus.APPROVED)
        ))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(((ServiceException) exception).getResultCode())
                        .isEqualTo("409-4"));
    }
}
