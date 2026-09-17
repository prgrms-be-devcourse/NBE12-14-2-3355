package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

// 프로필업데이트 요청 Dto
public record UpdateProfileRequestDto(
        @NotBlank
        String nickname, // 항상 값이 있어야함
        String profileImageUrl, // 프로필 사진 없어도됨
        String bio // 자기소개 없어도됨
) {
}
