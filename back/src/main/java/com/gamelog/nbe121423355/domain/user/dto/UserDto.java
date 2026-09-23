package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보")
public record UserDto(
        @Schema(description = "사용자 ID", example = "1")
        Long id,
        @Schema(description = "닉네임", example = "게임마스터")
        String nickname,
        @Schema(description = "이메일", example = "player@example.com")
        String email,
        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "자기소개", nullable = true)
        String bio,
        @Schema(description = "온보딩 완료 여부", example = "true")
        boolean onboardingCompleted,
        @Schema(description = "사용자 역할", example = "USER", allowableValues = {"USER", "ADMIN"})
        String role
) {
    public UserDto(User user) {
        this(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getBio(),
                user.isOnboardingCompleted(),
                user.getRole()
        );
    }
}
