package com.gamelog.nbe121423355.domain.game.dto;

import java.util.List;

public record GameFilterResponse(
        List<Option> genres,
        List<Option> platforms
) {
    public record Option(Long id, String name) {}
}
