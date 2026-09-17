package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    // 같은 사용자의 중복 신고 확인.
    boolean existsByReview_IdAndReporter_Id(Long reviewId, Long reporterId);

    // 처리 상태별 신고 목록 조회.
    Page<ReviewReport> findByStatus(ReportStatus status, Pageable pageable);
}
