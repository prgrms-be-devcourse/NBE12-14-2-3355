package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선호 게임 정보")
public record PreferredGameResponseDto(
        @Schema(description = "게임 ID", example = "6")
        Long gameId,
        @Schema(description = "게임명", example = "Baldur's Gate II: Shadows of Amn")
        String gameTitle
) {
    public PreferredGameResponseDto(UserPreferenceGame userPreferenceGame) {
        this(
                userPreferenceGame.getGame().getId(),
                userPreferenceGame.getGame().getTitle()
        );
    }
}
