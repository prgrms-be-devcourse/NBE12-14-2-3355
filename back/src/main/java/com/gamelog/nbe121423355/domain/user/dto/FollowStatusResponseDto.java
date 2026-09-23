package com.gamelog.nbe121423355.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팔로우 상태")
public record FollowStatusResponseDto(
        @Schema(description = "대상 사용자 ID", example = "2")
        Long targetUserId,
        @Schema(description = "현재 팔로우 여부", example = "true")
        boolean followed
) {
}
