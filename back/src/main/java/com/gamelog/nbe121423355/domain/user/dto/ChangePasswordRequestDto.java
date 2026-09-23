package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 비밀번호 변경 요청 dto
public record ChangePasswordRequestDto(
        @NotBlank
        String currentPassword,
        @NotBlank
        @Size(min = 8)
        String newPassword
) {
}
