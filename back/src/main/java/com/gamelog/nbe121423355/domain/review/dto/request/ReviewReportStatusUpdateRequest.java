package com.gamelog.nbe121423355.domain.review.dto.request;

import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewReportStatusUpdateRequest(
        @Schema(description = "변경할 신고 처리 상태", example = "APPROVED")
        @NotNull(message = "신고 처리 상태는 필수입니다.")
        ReportStatus status
) {
}
