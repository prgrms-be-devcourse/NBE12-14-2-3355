package com.gamelog.nbe121423355.domain.usergame.dto;

import java.math.BigDecimal;

public record UserGameGenreDistributionResponse(
        String genreName,
        BigDecimal ratio
) {
}
