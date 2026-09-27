package com.gamelog.nbe121423355.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "인기 리뷰 정보")
public record PopularReviewResponse(
        @Schema(description = "리뷰 ID", example = "13")
        Long reviewId,
        @Schema(description = "작성자 ID", example = "4")
        Long userId,
        @Schema(description = "작성자 닉네임", example = "게임마스터")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "게임 ID", example = "6")
        Long gameId,
        @Schema(description = "게임 제목", example = "Baldur's Gate II")
        String gameTitle,
        @Schema(description = "게임 커버 이미지 URL", nullable = true)
        String gameCoverImageUrl,
        @Schema(description = "별점", example = "4.5", nullable = true)
        BigDecimal rating,
        @Schema(description = "리뷰 내용", example = "정말 재미있게 플레이했습니다.")
        String content,
        @Schema(description = "스포일러 포함 여부", example = "false")
        boolean spoiler,
        @Schema(description = "좋아요 수", example = "12")
        long likeCount,
        @Schema(description = "작성 일시")
        LocalDateTime createdDate
) {
}
