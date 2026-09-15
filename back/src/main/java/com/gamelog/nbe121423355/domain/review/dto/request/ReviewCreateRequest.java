package com.gamelog.nbe121423355.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReviewCreateRequest(
        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        BigDecimal rating,

        String content,

        boolean spoiler
) {
}
