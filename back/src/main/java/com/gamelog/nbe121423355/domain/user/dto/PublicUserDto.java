package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.User;

public record PublicUserDto(
        Long id,
        String nickname,
        String profileImageUrl,
        String bio
) {
    public PublicUserDto(User user) {
        this(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBio()
        );
    }
}
