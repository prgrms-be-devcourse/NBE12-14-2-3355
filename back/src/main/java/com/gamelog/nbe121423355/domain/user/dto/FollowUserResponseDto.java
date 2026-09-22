package com.gamelog.nbe121423355.domain.user.dto;

import java.time.LocalDateTime;

public record FollowUserResponseDto(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime followedAt,
        boolean followedByMe,
        boolean me
) {
}
