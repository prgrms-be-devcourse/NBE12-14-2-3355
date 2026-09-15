package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 회원가입 요청 dto
public record SignupRequestDto(
        @NotBlank
        String nickname,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 8)
        String password
){ }
