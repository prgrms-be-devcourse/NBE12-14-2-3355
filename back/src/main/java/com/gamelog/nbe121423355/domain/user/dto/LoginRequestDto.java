package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.global.validation.MaxByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequestDto(
        @Schema(description = "가입 이메일", example = "player@example.com")
        @NotBlank
        String email,
        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank
        @MaxByteLength(value = 72, message = "비밀번호는 72바이트(영문 기준 72자) 이하여야 합니다.")
        String password
) {
}
