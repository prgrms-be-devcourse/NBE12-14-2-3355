package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

// 온보딩 선호 게임의 연관 추천 점수와 선호 장르 가산점을 계산
@Component
@RequiredArgsConstructor
public class PersonalizedOnboardingScoreCalculator {

    private final PersonalizedRelatedRankScoreCalculator relatedRankScoreCalculator;

    // 선호 게임별 연관 추천 순위 점수를 합산하고 직접 선택한 게임은 후보에서 제외
    public Map<Long, Integer> calculateRelatedScores(
            List<Long> preferredGameIds,
            List<PersonalizedRelatedGameProjection> relatedGames
    ) {
        Map<Long, Integer> scoresByGameId = new HashMap<>(relatedRankScoreCalculator.calculate(relatedGames));

        preferredGameIds.forEach(scoresByGameId::remove);

        return scoresByGameId;
    }

    // 후보 게임과 일치하는 선호 장르마다 10점씩, 최대 30점 부여
    public int calculateGenreBonus(
            List<Long> preferredGenreIds,
            List<Long> candidateGenreIds
    ) {
        Set<Long> preferredGenres = new HashSet<>(preferredGenreIds);

        long matchedGenreCount = candidateGenreIds.stream()
                .distinct()
                .filter(preferredGenres::contains)
                .count();

        return (int) Math.min(30, matchedGenreCount * 10);
    }


}
