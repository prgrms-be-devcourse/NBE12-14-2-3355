package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponseDto(
        @Schema(description = "로그인 사용자 정보")
        UserDto user,
        @Schema(description = "API 인증용 JWT accessToken", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {

}
