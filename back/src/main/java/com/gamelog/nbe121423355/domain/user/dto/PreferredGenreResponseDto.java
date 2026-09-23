package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선호 장르 정보")
public record PreferredGenreResponseDto(
        @Schema(description = "장르 ID", example = "12")
        Long genreId,
        @Schema(description = "장르명", example = "Role-playing (RPG)")
        String genreName
) {
    public PreferredGenreResponseDto(UserPreferenceGenre userPreferenceGenre) {
        this(
                userPreferenceGenre.getGenre().getId(),
                userPreferenceGenre.getGenre().getName()
        );
    }
}

