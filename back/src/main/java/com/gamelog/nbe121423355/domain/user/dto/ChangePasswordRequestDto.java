package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.global.validation.MaxByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 비밀번호 변경 요청 dto
@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequestDto(
        @NotBlank
        @MaxByteLength(value = 72, message = "비밀번호는 72바이트(영문 기준 72자) 이하여야 합니다.")
        @Schema(description = "현재 비밀번호", example = "currentPassword123!")
        String currentPassword,
        @NotBlank
        @Size(min = 8)
        @MaxByteLength(value = 72, message = "비밀번호는 72바이트(영문 기준 72자) 이하여야 합니다.")
        @Schema(description = "새 비밀번호(8자 이상, 72바이트 이하)", example = "newPassword123!", minLength = 8, maxLength = 72)
        String newPassword
) {
}
