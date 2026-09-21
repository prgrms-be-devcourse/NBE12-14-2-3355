package com.gamelog.nbe121423355.domain.game.service.model;

// 게임 기록에서 계산한 장르별 플레이 비율, 평균 별점, 선호도 점수
public record PersonalizedGenrePreference(
        Long genreId,
        double playRatio,
        Double averageRating,
        double score
) {
}