package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedGameRecordProjection;
import com.gamelog.nbe121423355.domain.game.service.model.PersonalizedGenrePreference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

// 게임 기록의 장르 기여도와 별점을 이용해 사용자 장르 선호도를 계산
@Component
public class PersonalizedGenrePreferenceCalculator {

    // 게임별 장르 기여도를 합산해 장르 선호도를 계산하고 높은 순서로 반환
    public List<PersonalizedGenrePreference> calculate(
            List<PersonalizedGameRecordProjection> records
    ) {
        Map<Long, List<PersonalizedGameRecordProjection>> recordsByGame = records.stream()
                .collect(Collectors.groupingBy(PersonalizedGameRecordProjection::gameId));

        Map<Long, GenreTotals> totalsByGenre = new HashMap<>();

        int gamesWithGenres = 0;

        for (List<PersonalizedGameRecordProjection> gameRecords : recordsByGame.values()) {
            List<Long> genreIds = gameRecords.stream()
                    .map(PersonalizedGameRecordProjection::genreId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (genreIds.isEmpty()) {
                continue;
            }

            gamesWithGenres++;
            double contribution = 1.0 / genreIds.size();
            BigDecimal rating = gameRecords.getFirst().rating();

            for (Long genreId : genreIds) {
                totalsByGenre
                        .computeIfAbsent(genreId, ignored -> new GenreTotals())
                        .add(contribution, rating);
            }
        }

        if (gamesWithGenres == 0) {
            return List.of();
        }

        int totalGamesWithGenres = gamesWithGenres;

        return totalsByGenre.entrySet().stream()
                .map(entry -> {
                    GenreTotals totals = entry.getValue();
                    double playRatio = totals.playWeight / totalGamesWithGenres;
                    Double averageRating = totals.averageRating();
                    double ratingScore = averageRating == null
                            ? 0.0
                            : averageRating / 5.0 * 60.0;

                    return new PersonalizedGenrePreference(
                            entry.getKey(),
                            playRatio,
                            averageRating,
                            playRatio * 40.0 + ratingScore
                    );
                })
                .sorted(Comparator
                        .comparingDouble(PersonalizedGenrePreference::score)
                        .reversed()
                        .thenComparing(PersonalizedGenrePreference::genreId))
                .toList();
    }

    private static class GenreTotals {

        private double playWeight;
        private double ratedWeight;
        private double weightedRatingSum;

        // 별점이 없어도 플레이 기여도는 더하고, 별점 평균에는 포함하지 않음
        private void add(double contribution, BigDecimal rating) {
            playWeight += contribution;

            if (rating != null) {
                ratedWeight += contribution;
                weightedRatingSum += rating.doubleValue() * contribution;
            }
        }

        // 해당 장르에 별점이 있는 기록이 없으면 평균 별점을 반환하지 않음
        private Double averageRating() {
            return ratedWeight == 0.0
                    ? null
                    : weightedRatingSum / ratedWeight;
        }

    }

}
