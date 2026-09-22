package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import org.springframework.data.domain.Page;

import java.util.List;

public record FollowPageResponseDto(
        List<FollowUserResponseDto> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static FollowPageResponseDto from(
            Page<UserFollow> followPage,
            List<FollowUserResponseDto> users
    ) {
        return new FollowPageResponseDto(
                users,
                followPage.getNumber(),
                followPage.getSize(),
                followPage.getTotalElements(),
                followPage.getTotalPages(),
                followPage.hasNext()
        );
    }

}
