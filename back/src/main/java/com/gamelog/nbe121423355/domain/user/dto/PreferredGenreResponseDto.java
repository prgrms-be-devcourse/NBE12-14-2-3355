package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;

public record PreferredGenreResponseDto(
        Long genreId,
        String genreName
) {
    public PreferredGenreResponseDto(UserPreferenceGenre userPreferenceGenre) {
        this(
                userPreferenceGenre.getGenre().getId(),
                userPreferenceGenre.getGenre().getName()
        );
    }
}

