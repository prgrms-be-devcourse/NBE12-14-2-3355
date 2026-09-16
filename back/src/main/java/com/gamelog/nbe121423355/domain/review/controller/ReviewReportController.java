package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportCreateRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewReportStatusUpdateRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewReportPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewReportResponse;
import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.service.ReviewReportService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    @PostMapping("/reviews/{reviewId}/reports")
    public RsData<ReviewReportResponse> createReport(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReportCreateRequest request
    ) {
        ReviewReportResponse response = reviewReportService.createReport(
                securityUser.getId(),
                reviewId,
                request
        );
        return new RsData<>("201-1", "리뷰 신고가 접수되었습니다.", response);
    }

    @GetMapping("/admin/review-reports")
    public RsData<ReviewReportPageResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdDate", direction = DESC) Pageable pageable
    ) {
        ReviewReportPageResponse response = reviewReportService.getReports(status, pageable);
        return new RsData<>("200-7", "리뷰 신고 목록을 조회했습니다.", response);
    }

    @PutMapping("/admin/review-reports/{reportId}/status")
    public RsData<ReviewReportResponse> updateStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReviewReportStatusUpdateRequest request
    ) {
        ReviewReportResponse response = reviewReportService.updateStatus(reportId, request);
        return new RsData<>("200-8", "리뷰 신고 상태를 변경했습니다.", response);
    }
}
