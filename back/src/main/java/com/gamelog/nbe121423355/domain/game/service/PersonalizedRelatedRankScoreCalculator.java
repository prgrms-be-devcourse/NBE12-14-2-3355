package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 기준 게임별 연관 추천 순위를 점수로 바꾸고 후보 게임별로 합산
@Component
public class PersonalizedRelatedRankScoreCalculator {

    // 각 기준 게임의 연관 추천 순위를 점수로 바꿔 후보 게임 ID별로 합산
    public Map<Long, Integer> calculate(List<PersonalizedRelatedGameProjection> relatedGames) {
        Map<Long, Integer> scoresByGameId = new HashMap<>();

        for (PersonalizedRelatedGameProjection relatedGame : relatedGames) {
            scoresByGameId.merge(
                    relatedGame.gameId(),
                    scoreForRank(relatedGame.relatedRank()),
                    Integer::sum
            );
        }

        return scoresByGameId;
    }

    // 연관 추천 1~5위에 약속한 점수를 부여
    private int scoreForRank(int rank) {
        return switch (rank) {
            case 1 -> 20;
            case 2 -> 17;
            case 3 -> 14;
            case 4 -> 11;
            case 5 -> 8;
            default -> throw new IllegalArgumentException("지원하지 않는 연관 추천 순위: " + rank);
        };
    }

}
