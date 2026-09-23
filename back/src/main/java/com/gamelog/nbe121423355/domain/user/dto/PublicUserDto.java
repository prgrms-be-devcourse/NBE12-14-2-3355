package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 공개 프로필 정보")
public record PublicUserDto(
        @Schema(description = "사용자 ID", example = "2")
        Long id,
        @Schema(description = "닉네임", example = "게임친구")
        String nickname,
        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "자기소개", nullable = true)
        String bio
) {
    public PublicUserDto(User user) {
        this(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBio()
        );
    }
}
