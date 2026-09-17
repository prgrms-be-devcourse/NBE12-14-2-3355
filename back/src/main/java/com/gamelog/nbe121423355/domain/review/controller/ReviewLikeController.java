package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.response.ReviewLikeResponse;
import com.gamelog.nbe121423355.domain.review.service.ReviewLikeService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
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
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    @PostMapping
    public RsData<ReviewLikeResponse> addLike(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        ReviewLikeResponse response = reviewLikeService.addLike(securityUser.getId(), reviewId);
        return new RsData<>("201-2", "리뷰에 좋아요를 등록했습니다.", response);
    }

    @DeleteMapping
    public RsData<ReviewLikeResponse> removeLike(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        ReviewLikeResponse response = reviewLikeService.removeLike(securityUser.getId(), reviewId);
        return new RsData<>("200-9", "리뷰 좋아요를 취소했습니다.", response);
    }

    @GetMapping("/count")
    public RsData<ReviewLikeResponse> getLikeStatus(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        ReviewLikeResponse response = reviewLikeService.getLikeStatus(securityUser.getId(), reviewId);
        return new RsData<>("200-10", "리뷰 좋아요 정보를 조회했습니다.", response);
    }
}
