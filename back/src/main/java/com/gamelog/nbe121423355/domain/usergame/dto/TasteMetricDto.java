package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record TasteMetricDto(
        BigDecimal ratio,
        String message,
        String description
) {
}
