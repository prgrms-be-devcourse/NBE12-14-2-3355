package com.gamelog.nbe121423355.domain.usergame.dto;

public record UserGameGenreDTO(
        Long genreId,
        String genreName,
        Long gameCount
) {
}