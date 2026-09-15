package com.gamelog.nbe121423355.domain.game.dto;

import java.math.BigDecimal;
import java.util.List;

public record IgdbGameResponse(
        Long id,
        String name,
        String summary,
        Cover cover,
        Long first_release_date,
        BigDecimal rating,
        List<InvolvedCompany> involved_companies,
        List<NamedResource> genres,
        List<NamedResource> platforms,
        List<NamedResource> collections
) {
    public record Cover(Long id, String url) {}

    public record InvolvedCompany(
            Long id,
            Boolean developer,
            Company company
    ) {}

    public record Company(Long id, String name) {}

    public record NamedResource(Long id, String name) {}
}