package com.gamelog.nbe121423355.domain.user.dto;

import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "팔로우 사용자 페이지")
public record FollowPageResponseDto(
        @Schema(description = "사용자 목록")
        List<FollowUserResponseDto> users,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 사용자 수", example = "24")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "3")
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
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
