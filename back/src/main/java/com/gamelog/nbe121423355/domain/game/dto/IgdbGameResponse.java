package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;

public record IgdbGameResponse(
        Long id,
        String name,
        String summary,
        Cover cover,
        Long first_release_date,
        BigDecimal rating
) {
    public record Cover(
            Long id,
            String url
    ) {}
}
