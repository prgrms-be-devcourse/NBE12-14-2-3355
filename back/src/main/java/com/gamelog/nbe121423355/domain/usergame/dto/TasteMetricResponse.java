package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record TasteMetricResponse(
        BigDecimal ratio,
        String message,
        String description
) {
}
