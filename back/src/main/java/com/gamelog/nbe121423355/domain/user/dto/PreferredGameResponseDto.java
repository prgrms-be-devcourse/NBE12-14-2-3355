package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;

// 선호 게임 응답DTO
public record PreferredGameResponseDto(
        Long gameId,
        String gameTitle
) {
    public PreferredGameResponseDto(UserPreferenceGame userPreferenceGame) {
        this(
                userPreferenceGame.getGame().getId(),
                userPreferenceGame.getGame().getTitle()
        );
    }
}
