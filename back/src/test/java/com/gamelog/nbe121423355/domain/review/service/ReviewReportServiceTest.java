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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
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
        ReflectionTestUtils.setField(review, "id", 10L);
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

    @DisplayName("같은 리뷰의 여러 신고를 하나의 묶음으로 조회한다")
    @Test
    void getReportsGroupedByReview() {
        User anotherReporter = mock(User.class);
        when(anotherReporter.getId()).thenReturn(3L);
        when(anotherReporter.getNickname()).thenReturn("다른신고자");
        ReviewReport firstReport = new ReviewReport(review, reporter, "첫 번째 신고");
        ReviewReport secondReport = new ReviewReport(review, anotherReporter, "두 번째 신고");
        PageRequest pageable = PageRequest.of(0, 20);

        when(reviewReportRepository.findReviewIdsGroupedByReview(ReportStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(10L), pageable, 1));
        when(reviewReportRepository.findAllByReviewIdsAndStatus(
                List.of(10L),
                ReportStatus.PENDING
        )).thenReturn(List.of(firstReport, secondReport));

        var response = reviewReportService.getReports(ReportStatus.PENDING, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.reports()).hasSize(1);
        assertThat(response.reports().get(0).reviewId()).isEqualTo(10L);
        assertThat(response.reports().get(0).reportCount()).isEqualTo(2);
        assertThat(response.reports().get(0).pendingReportCount()).isEqualTo(2);
        assertThat(response.reports().get(0).reports()).hasSize(2);
    }

    @DisplayName("신고를 승인하면 리뷰를 관리자 숨김 처리한다")
    @Test
    void approveReportHidesReview() {
        ReviewReport report = new ReviewReport(review, reporter, "부적절한 내용");
        User anotherReporter = mock(User.class);
        lenient().when(anotherReporter.getId()).thenReturn(3L);
        lenient().when(anotherReporter.getNickname()).thenReturn("다른신고자");
        ReviewReport anotherReport = new ReviewReport(review, anotherReporter, "같은 리뷰의 다른 신고");
        when(reviewReportRepository.findById(20L)).thenReturn(Optional.of(report));
        when(reviewReportRepository.findByReview_IdAndStatus(10L, ReportStatus.PENDING))
                .thenReturn(List.of(report, anotherReport));

        var response = reviewReportService.updateStatus(
                20L,
                new ReviewReportStatusUpdateRequest(ReportStatus.APPROVED)
        );

        assertThat(response.status()).isEqualTo(ReportStatus.APPROVED);
        assertThat(anotherReport.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN_BY_ADMIN);
        assertThat(review.getDeletedAt()).isNotNull();
    }

    @DisplayName("신고를 반려하면 리뷰 공개 상태를 유지한다")
    @Test
    void rejectReportKeepsReviewActive() {
        ReviewReport report = new ReviewReport(review, reporter, "문제 없음");
        when(reviewReportRepository.findById(20L)).thenReturn(Optional.of(report));
        when(reviewReportRepository.findByReview_IdAndStatus(10L, ReportStatus.PENDING))
                .thenReturn(List.of(report));

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
