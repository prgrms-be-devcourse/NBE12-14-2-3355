package com.gamelog.nbe121423355.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 선호 게임 요청Dto
public record PreferredGameRequestDto(
        @NotEmpty
        List<Long> gameIds
) {

}
