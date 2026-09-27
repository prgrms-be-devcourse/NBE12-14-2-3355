package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequestDto(
        @Schema(description = "가입 이메일", example = "player@example.com")
        @NotBlank
        String email,
        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank
        String password
) {
}
