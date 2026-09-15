package com.gamelog.nbe121423355.domain.user.dto;


// 로그인 응답 dto
public record LoginResponse(
        UserDto user,
        String accessToken
) {

}
