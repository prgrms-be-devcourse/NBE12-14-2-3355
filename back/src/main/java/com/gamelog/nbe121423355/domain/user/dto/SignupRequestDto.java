package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.global.validation.MaxByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequestDto(
        @Schema(description = "닉네임", example = "게임마스터")
        @NotBlank
        String nickname,
        @Schema(description = "이메일", example = "player@example.com")
        @NotBlank
        @Email
        String email,
        @Schema(description = "비밀번호(8자 이상, 72바이트 이하)", example = "password123!", minLength = 8, maxLength = 72)
        @NotBlank
        @Size(min = 8)
        @MaxByteLength(value = 72, message = "비밀번호는 72바이트(영문 기준 72자) 이하여야 합니다.")
        String password
){ }
