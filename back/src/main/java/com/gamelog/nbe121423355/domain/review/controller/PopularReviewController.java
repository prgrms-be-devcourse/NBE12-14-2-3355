package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.response.PopularReviewResponse;
import com.gamelog.nbe121423355.domain.review.service.PopularReviewService;
import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class PopularReviewController {

    private final PopularReviewService popularReviewService;

    @GetMapping("/popular")
    public RsData<List<PopularReviewResponse>> getPopularReviews(
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