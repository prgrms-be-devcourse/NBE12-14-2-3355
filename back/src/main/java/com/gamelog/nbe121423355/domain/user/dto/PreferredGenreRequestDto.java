package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 선호 장르 오청Dto
public record PreferredGenreRequestDto(
        @Schema(description = "선호 장르 ID 목록", example = "[12, 31, 36]")
        @NotEmpty
        List<Long> genreIds
) {
}
