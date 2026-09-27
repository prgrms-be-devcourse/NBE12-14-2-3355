package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "게임 검색 필터 목록")
public record GameFilterResponse(
        @Schema(description = "장르 선택지")
        List<Option> genres,
        @Schema(description = "플랫폼 선택지")
        List<Option> platforms
) {
    @Schema(description = "필터 선택지")
    public record Option(
            @Schema(description = "선택지 ID", example = "12") Long id,
            @Schema(description = "표시 이름", example = "Role-playing (RPG)") String name
    ) {}
}
