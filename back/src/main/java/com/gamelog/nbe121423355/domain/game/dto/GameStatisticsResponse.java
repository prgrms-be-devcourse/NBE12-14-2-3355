package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "게임 커뮤니티 기록 통계")
public record GameStatisticsResponse(
        @Schema(description = "플레이 완료 사용자 수", example = "120")
        long playedCount,
        @Schema(description = "플레이 중 사용자 수", example = "15")
        long playingCount,
        @Schema(description = "플레이 예정 사용자 수", example = "32")
        long backlogCount,
        @Schema(description = "위시리스트 사용자 수", example = "41")
        long wishlistCount,
        @Schema(description = "좋아하는 게임 등록 수", example = "87")
        long likeCount,
        @Schema(description = "사용자 평균 별점", example = "4.3")
        BigDecimal averageRating,
        @Schema(description = "별점 등록 수", example = "56")
        long ratingCount,
        @Schema(description = "내용이 작성된 리뷰 수", example = "31")
        long reviewCount,
        @Schema(description = "별점별 분포")
        List<GameRatingDistributionResponse> ratingDistribution,
        @Schema(description = "평균 플레이 시간", example = "42.5", nullable = true)
        BigDecimal averagePlayTimeHours,
        @Schema(description = "플레이 시간 기록 사용자 수", example = "20")
        long playTimeUserCount
) {
}
