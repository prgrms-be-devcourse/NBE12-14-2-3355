package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 선호 장르 오청Dto
public record PreferredGenreRequestDto(
        @NotEmpty
        List<Long> genreIds
) {
}
