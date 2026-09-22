package com.gamelog.nbe121423355.domain.user.dto;

public record FollowStatusResponseDto(
        Long targetUserId,
        boolean followed
) {
}
