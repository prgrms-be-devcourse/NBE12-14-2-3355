package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.response.ReviewLikeResponse;
import com.gamelog.nbe121423355.domain.review.service.ReviewLikeService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/likes")
@RequiredArgsConstructor
@Tag(name = "review-like-controller", description = "리뷰 좋아요 등록·취소·조회 API")
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    @PostMapping
    @Operation(summary = "리뷰 좋아요 등록", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ReviewLikeResponse> addLike(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        ReviewLikeResponse response = reviewLikeService.addLike(securityUser.getId(), reviewId);
        return new RsData<>("201-2", "리뷰에 좋아요를 등록했습니다.", response);
    }

    @DeleteMapping
    @Operation(summary = "리뷰 좋아요 취소", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ReviewLikeResponse> removeLike(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        ReviewLikeResponse response = reviewLikeService.removeLike(securityUser.getId(), reviewId);
        return new RsData<>("200-9", "리뷰 좋아요를 취소했습니다.", response);
    }

    @GetMapping("/count")
    @Operation(summary = "리뷰 좋아요 정보 조회", description = "비로그인 사용자도 좋아요 수를 조회할 수 있으며, 로그인 시 본인의 좋아요 여부도 반환합니다.")
    public RsData<ReviewLikeResponse> getLikeStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        Long userId = securityUser == null ? null : securityUser.getId();
        ReviewLikeResponse response = reviewLikeService.getLikeStatus(userId, reviewId);
        return new RsData<>("200-10", "리뷰 좋아요 정보를 조회했습니다.", response);
    }
}
