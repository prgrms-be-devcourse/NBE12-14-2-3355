package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.User;

// user 응답 dto
public record UserDto(
        Long id,
        String nickname,
        String email,
        String profileImageUrl,
        String bio,
        boolean onboardingCompleted,
        String role
) {
    public UserDto(User user) {
        this(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getBio(),
                user.isOnboardingCompleted(),
                user.getRole()
        );
    }
}
