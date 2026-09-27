package com.gamelog.nbe121423355.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReportCreateRequest(
        @Schema(description = "신고 사유", example = "욕설 및 비방이 포함된 리뷰입니다.", maxLength = 255)
        @NotBlank(message = "신고 사유는 필수입니다.")
        @Size(max = 255, message = "신고 사유는 255자를 초과할 수 없습니다.")
        String reason
) {
}
