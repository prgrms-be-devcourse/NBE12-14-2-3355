package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.FollowPageResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.FollowStatusResponseDto;
import com.gamelog.nbe121423355.domain.user.service.UserFollowService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "user-follow-controller", description = "사용자 검색 및 팔로우 관계 API")
public class UserFollowController {

    private final UserFollowService userFollowService;

    @GetMapping("/search")
    @Operation(summary = "사용자 검색", description = "닉네임으로 사용자를 검색하고 로그인 사용자의 팔로우 여부를 함께 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<FollowPageResponseDto> searchUsers(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "검색할 닉네임", example = "게임")
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return new RsData<>("200-18", "사용자 검색 결과를 조회했습니다.",
                userFollowService.searchUsers(securityUser == null ? null : securityUser.getId(), keyword, page, size));
    }

    @PutMapping("/me/following/{targetUserId}")
    @Operation(summary = "사용자 팔로우", description = "로그인 사용자가 대상 사용자를 팔로우합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<FollowStatusResponseDto> follow(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "팔로우할 사용자 ID", example = "2")
            @PathVariable Long targetUserId
    ) {
        FollowStatusResponseDto response =
                userFollowService.follow(
                        securityUser.getId(),
                        targetUserId
                );

        return new RsData<>(
                "200-14",
                "사용자를 팔로우했습니다.",
                response
        );
    }

    @DeleteMapping("/me/following/{targetUserId}")
    @Operation(summary = "사용자 팔로우 취소", description = "로그인 사용자가 대상 사용자 팔로우를 취소합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<FollowStatusResponseDto> unfollow(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "팔로우를 취소할 사용자 ID", example = "2")
            @PathVariable Long targetUserId
    ) {
        FollowStatusResponseDto response =
                userFollowService.unfollow(
                        securityUser.getId(),
                        targetUserId
                );

        return new RsData<>(
                "200-15",
                "사용자 팔로우를 취소했습니다.",
                response
        );
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 사용자 목록을 페이지 단위로 조회합니다.")
    public RsData<FollowPageResponseDto> getFollowing(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "조회할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long loginUserId = securityUser == null
                ? null
                : securityUser.getId();

        FollowPageResponseDto response =
                userFollowService.getFollowing(
                        loginUserId,
                        userId,
                        page,
                        size
                );

        return new RsData<>(
                "200-16",
                "팔로잉 목록을 조회했습니다.",
                response
        );
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "팔로워 목록 조회", description = "특정 사용자를 팔로우하는 사용자 목록을 페이지 단위로 조회합니다.")
    public RsData<FollowPageResponseDto> getFollowers(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "조회할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long loginUserId = securityUser == null
                ? null
                : securityUser.getId();

        FollowPageResponseDto response =
                userFollowService.getFollowers(
                        loginUserId,
                        userId,
                        page,
                        size
                );

        return new RsData<>(
                "200-17",
                "팔로워 목록을 조회했습니다.",
                response
        );
    }
}
