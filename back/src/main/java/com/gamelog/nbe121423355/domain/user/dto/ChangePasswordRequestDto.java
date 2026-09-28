package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 비밀번호 변경 요청 dto
@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequestDto(
        @NotBlank
        @Schema(description = "현재 비밀번호", example = "currentPassword123!")
        String currentPassword,
        @NotBlank
        @Size(min = 8)
        @Schema(description = "새 비밀번호", example = "newPassword123!", minLength = 8)
        String newPassword
) {
}
