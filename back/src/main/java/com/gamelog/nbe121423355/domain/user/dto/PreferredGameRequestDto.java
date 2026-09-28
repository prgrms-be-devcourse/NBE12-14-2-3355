package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 선호 게임 요청Dto
public record PreferredGameRequestDto(
        @Schema(description = "선호 게임 ID 목록", example = "[6, 9, 12]")
        @NotEmpty
        List<Long> gameIds
) {

}
