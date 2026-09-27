package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.response.PopularReviewResponse;
import com.gamelog.nbe121423355.domain.review.service.PopularReviewService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "popular-review-controller", description = "인기 리뷰 조회 API")
public class PopularReviewController {

    private final PopularReviewService popularReviewService;

    @GetMapping("/popular")
    @Operation(summary = "인기 리뷰 조회", description = "좋아요 수가 많은 공개 리뷰를 조회합니다.")
    public RsData<List<PopularReviewResponse>> getPopularReviews(
            @Parameter(description = "조회할 리뷰 수", example = "5")
            @RequestParam(defaultValue = "5") int size
    ) {
        List<PopularReviewResponse> response =
                popularReviewService.getPopularReviews(size);

        return new RsData<>(
                "200-1",
                "인기 리뷰를 조회했습니다.",
                response
        );
    }
}
