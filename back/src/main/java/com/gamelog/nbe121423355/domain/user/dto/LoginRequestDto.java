package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

// 로그인 요청 dto
public record LoginRequestDto(
        @NotBlank
        String email,
        @NotBlank
        String password
) {
}
