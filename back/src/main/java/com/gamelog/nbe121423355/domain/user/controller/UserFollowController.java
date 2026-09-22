package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.FollowPageResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.FollowStatusResponseDto;
import com.gamelog.nbe121423355.domain.user.service.UserFollowService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserFollowController {

    private final UserFollowService userFollowService;

    @PutMapping("/me/following/{targetUserId}")
    public RsData<FollowStatusResponseDto> follow(
            @AuthenticationPrincipal SecurityUser securityUser,
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
    public RsData<FollowStatusResponseDto> unfollow(
            @AuthenticationPrincipal SecurityUser securityUser,
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
    public RsData<FollowPageResponseDto> getFollowing(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
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
    public RsData<FollowPageResponseDto> getFollowers(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
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