package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.review.service.ReviewService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "review-controller", description = "게임 리뷰 작성·조회·수정·삭제 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PutMapping("/games/{gameId}/reviews")
    @Operation(summary = "게임 기록 및 리뷰 저장", description = "내 게임 기록과 리뷰를 한 번에 생성하거나 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<DetailedReviewResponse> saveDetailedReview(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long gameId,
            @Valid @RequestBody DetailedReviewSaveRequest request
    ) {
        DetailedReviewResponse response = reviewService.saveDetailedReview(
                securityUser.getId(),
                gameId,
                request
        );
        return new RsData<>("200-7", "상세 리뷰가 저장되었습니다.", response);
    }

    @GetMapping("/games/{gameId}/reviews/me")
    @Operation(summary = "내 게임 기록 및 리뷰 조회", description = "해당 게임에 저장한 내 기록과 리뷰를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<DetailedReviewResponse> getMyDetailedReview(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long gameId
    ) {
        DetailedReviewResponse response = reviewService.getMyDetailedReview(
                securityUser.getId(),
                gameId
        );
        return new RsData<>("200-8", "내 상세 리뷰 조회에 성공했습니다.", response);
    }

    @PutMapping("/user-games/{userGameId}/reviews")
    @Operation(summary = "리뷰 저장", description = "사용자 게임 기록에 리뷰를 생성하거나 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ReviewResponse> saveReview(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long userGameId,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        ReviewResponse response = reviewService.saveReview(securityUser.getId(), userGameId, request);
        return new RsData<>("200-6", "리뷰가 저장되었습니다.", response);
    }

    @GetMapping("/reviews/{reviewId}")
    @Operation(summary = "리뷰 단건 조회", description = "리뷰 ID로 공개 중인 리뷰를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ReviewResponse> getReview(@PathVariable Long reviewId) {
        ReviewResponse response = reviewService.getReview(reviewId);
        return new RsData<>("200-1", "리뷰 조회에 성공했습니다.", response);
    }

    @GetMapping("/games/{gameId}/reviews")
    @Operation(summary = "게임 리뷰 목록 조회", description = "게임에 작성된 공개 리뷰를 페이지 단위로 조회합니다.")
    public RsData<ReviewPageResponse> getGameReviews(
            @PathVariable Long gameId,
            @PageableDefault(size = 20, sort = "createdDate", direction = DESC) Pageable pageable
    ) {
        ReviewPageResponse response = reviewService.getGameReviews(gameId, pageable);
        return new RsData<>("200-2", "게임 리뷰 목록 조회에 성공했습니다.", response);
    }

    @GetMapping("/users/{userId}/reviews")
    @Operation(summary = "사용자 리뷰 목록 조회", description = "특정 사용자가 작성한 공개 리뷰를 페이지 단위로 조회합니다.")
    public RsData<ReviewPageResponse> getUserReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdDate", direction = DESC) Pageable pageable
    ) {
        ReviewPageResponse response = reviewService.getUserReviews(userId, pageable);
        return new RsData<>("200-3", "사용자 리뷰 목록 조회에 성공했습니다.", response);
    }

    @PutMapping("/reviews/{reviewId}")
    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰의 별점, 내용과 스포일러 여부를 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ReviewResponse> updateReview(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(securityUser.getId(), reviewId, request);
        return new RsData<>("200-4", "리뷰가 수정되었습니다.", response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 소프트 삭제합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<Void> deleteReview(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(securityUser.getId(), reviewId);
        return new RsData<>("200-5", "리뷰가 삭제되었습니다.");
    }
}
