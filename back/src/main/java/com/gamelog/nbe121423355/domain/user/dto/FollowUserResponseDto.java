package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "팔로우 목록 사용자 정보")
public record FollowUserResponseDto(
        @Schema(description = "사용자 ID", example = "2")
        Long userId,
        @Schema(description = "닉네임", example = "게임친구")
        String nickname,
        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "팔로우 일시")
        LocalDateTime followedAt,
        @Schema(description = "로그인 사용자의 팔로우 여부", example = "true")
        boolean followedByMe,
        @Schema(description = "로그인 사용자 본인 여부", example = "false")
        boolean me
) {
}
