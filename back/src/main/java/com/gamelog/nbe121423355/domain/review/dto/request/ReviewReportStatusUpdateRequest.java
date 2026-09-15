package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewReportStatusUpdateRequest(
        @NotNull(message = "신고 처리 상태는 필수입니다.")
        ReportStatus status
) {
}
