package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequestDto(
        @Schema(description = "변경할 닉네임", example = "게임마스터")
        @NotBlank
        String nickname, // 항상 값이 있어야함
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
        String profileImageUrl, // 프로필 사진 없어도됨
        @Schema(description = "자기소개", example = "RPG와 전략 게임을 좋아합니다.", nullable = true)
        String bio // 자기소개 없어도됨
) {
}
