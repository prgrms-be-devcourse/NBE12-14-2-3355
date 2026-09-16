package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.review.service.ReviewService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PutMapping("/user-games/{userGameId}/reviews")
    public RsData<ReviewResponse> saveReview(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long userGameId,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        ReviewResponse response = reviewService.saveReview(securityUser.getId(), userGameId, request);
        return new RsData<>("200-6", "리뷰가 저장되었습니다.", response);
    }

    @GetMapping("/reviews/{reviewId}")
    public RsData<ReviewResponse> getReview(@PathVariable Long reviewId) {
        ReviewResponse response = reviewService.getReview(reviewId);
        return new RsData<>("200-1", "리뷰 조회에 성공했습니다.", response);
    }

    @GetMapping("/games/{gameId}/reviews")
    public RsData<ReviewPageResponse> getGameReviews(
            @PathVariable Long gameId,
            @PageableDefault(size = 20, sort = "createdDate", direction = DESC) Pageable pageable
    ) {
        ReviewPageResponse response = reviewService.getGameReviews(gameId, pageable);
        return new RsData<>("200-2", "게임 리뷰 목록 조회에 성공했습니다.", response);
    }

    @GetMapping("/users/{userId}/reviews")
    public RsData<ReviewPageResponse> getUserReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdDate", direction = DESC) Pageable pageable
    ) {
        ReviewPageResponse response = reviewService.getUserReviews(userId, pageable);
        return new RsData<>("200-3", "사용자 리뷰 목록 조회에 성공했습니다.", response);
    }

    @PutMapping("/reviews/{reviewId}")
    public RsData<ReviewResponse> updateReview(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(securityUser.getId(), reviewId, request);
        return new RsData<>("200-4", "리뷰가 수정되었습니다.", response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public RsData<Void> deleteReview(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(securityUser.getId(), reviewId);
        return new RsData<>("200-5", "리뷰가 삭제되었습니다.");
    }
}
