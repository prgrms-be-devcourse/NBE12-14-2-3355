package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.PersonalizedGameRecommendationResponse;
import com.gamelog.nbe121423355.domain.game.repository.PersonalizedGameRecordQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.PersonalizedOnboardingPreferenceQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.PersonalizedRecommendationCandidateQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.PersonalizedRelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedGameRecordProjection;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRecommendationCandidateProjection;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import com.gamelog.nbe121423355.domain.game.service.model.PersonalizedGenrePreference;
import com.gamelog.nbe121423355.domain.game.service.model.PersonalizedRecommendationGenre;
import com.gamelog.nbe121423355.domain.game.service.model.PersonalizedRecommendationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

// 게임 기록을 우선하고 온보딩을 후순위로 사용해 맞춤 추천 결과를 생성
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedGameRecommendationService {

    private static final BigDecimal IGDB_RATING_WEIGHT = new BigDecimal("0.5");

    private final PersonalizedGameRecordQueryRepository gameRecordQueryRepository;
    private final PersonalizedOnboardingPreferenceQueryRepository onboardingPreferenceQueryRepository;
    private final PersonalizedRelatedGameQueryRepository relatedGameQueryRepository;
    private final PersonalizedRecommendationCandidateQueryRepository candidateQueryRepository;
    private final PersonalizedGenrePreferenceCalculator genrePreferenceCalculator;
    private final PersonalizedRecordSourceSelector recordSourceSelector;
    private final PersonalizedRelatedRankScoreCalculator relatedRankScoreCalculator;
    private final PersonalizedOnboardingScoreCalculator onboardingScoreCalculator;

    // 사용자별 맞춤 추천 결과를 API 응답 DTO로 변환해 반환
    public List<PersonalizedGameRecommendationResponse> recommend(Long userId) {
        return recommendResults(userId).stream()
                .map(result -> new PersonalizedGameRecommendationResponse(
                        result.gameId(),
                        result.title(),
                        result.coverImageUrl(),
                        result.igdbRating(),
                        result.recommendationScore(),
                        result.genres().stream()
                                .map(genre -> new GameDetailResponse.GenreResponse(
                                        genre.id(),
                                        genre.name()
                                ))
                                .toList()
                ))
                .toList();
    }

    // 사용 가능한 게임 기록을 우선하고, 사용할 수 없으면 온보딩 추천으로 전환
    private List<PersonalizedRecommendationResult> recommendResults(Long userId) {
        List<PersonalizedGameRecordProjection> records =
                gameRecordQueryRepository.findEligibleRecordsByUserId(userId);

        if (!records.isEmpty()) {
            List<Long> sourceGameIds =
                    recordSourceSelector.selectSourceGameIds(records);

            if (!sourceGameIds.isEmpty()) {
                return recommendFromRecords(records, sourceGameIds);
            }
        }

        return recommendFromOnboarding(userId);
    }

    // 기록 게임의 연관 추천 순위 점수와 상위 장르 가산점으로 추천 결과 생성
    private List<PersonalizedRecommendationResult> recommendFromRecords(
            List<PersonalizedGameRecordProjection> records,
            List<Long> sourceGameIds
    ) {
        List<PersonalizedRelatedGameProjection> relatedGames =
                relatedGameQueryRepository.findTopFiveBySourceGameIds(sourceGameIds);

        Map<Long, Integer> relatedScores =
                relatedRankScoreCalculator.calculate(relatedGames);

        if (relatedScores.isEmpty()) {
            return List.of();
        }

        List<PersonalizedGenrePreference> topGenres =
                genrePreferenceCalculator.calculate(records).stream()
                        .limit(2)
                        .toList();

        return buildResults(
                relatedScores,
                genreIds -> calculateRecordGenreBonus(topGenres, genreIds)
        );
    }

    // 온보딩 선호 게임을 우선 후보로 사용하고, 선호 게임이 없으면 장르 후보를 조회
    private List<PersonalizedRecommendationResult> recommendFromOnboarding(Long userId) {
        List<Long> preferredGameIds = onboardingPreferenceQueryRepository.findPreferredGameIds(userId);

        List<Long> preferredGenreIds = onboardingPreferenceQueryRepository.findPreferredGenreIds(userId);

        if (preferredGameIds.isEmpty() && preferredGenreIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> relatedScores;

        if (!preferredGameIds.isEmpty()) {
            List<PersonalizedRelatedGameProjection> relatedGames =
                    relatedGameQueryRepository.findTopFiveBySourceGameIds(preferredGameIds);

            relatedScores = onboardingScoreCalculator.calculateRelatedScores(
                    preferredGameIds,
                    relatedGames
            );
        } else {
            List<Long> genreCandidateIds =
                    candidateQueryRepository.findTopFiveIdsByPreferredGenres(preferredGenreIds);

            relatedScores = genreCandidateIds.stream()
                    .collect(Collectors.toMap(
                            gameId -> gameId,
                            gameId -> 0
                    ));
        }

        if (relatedScores.isEmpty()) {
            return List.of();
        }

        return buildResults(
                relatedScores,
                genreIds -> onboardingScoreCalculator.calculateGenreBonus(
                        preferredGenreIds,
                        genreIds
                )
        );
    }

    // 후보 정보를 일괄 조회하고 최종 추천 점수를 계산해 상위 5개 반환
    private List<PersonalizedRecommendationResult> buildResults(
            Map<Long, Integer> relatedScores,
            ToIntFunction<List<Long>> genreBonusCalculator
    ) {
        List<PersonalizedRecommendationCandidateProjection> candidateRows =
                candidateQueryRepository.findAllByGameIds(
                        new ArrayList<>(relatedScores.keySet())
                );

        Map<Long, List<PersonalizedRecommendationCandidateProjection>> rowsByGameId =
                candidateRows.stream()
                        .collect(Collectors.groupingBy(
                                PersonalizedRecommendationCandidateProjection::gameId
                        ));

        return relatedScores.entrySet().stream()
                .map(entry -> createResult(
                        entry.getKey(),
                        entry.getValue(),
                        rowsByGameId.get(entry.getKey()),
                        genreBonusCalculator
                ))
                .filter(Objects::nonNull)
                .sorted(resultComparator())
                .limit(5)
                .toList();
    }

    // 한 후보의 순위 점수, 장르 가산점, IGDB 평점 점수를 합산
    private PersonalizedRecommendationResult createResult(
            Long gameId,
            int relatedScore,
            List<PersonalizedRecommendationCandidateProjection> rows,
            ToIntFunction<List<Long>> genreBonusCalculator
    ) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        PersonalizedRecommendationCandidateProjection game = rows.getFirst();

        List<PersonalizedRecommendationGenre> genres = rows.stream()
                .filter(row -> row.genreId() != null)
                .map(row -> new PersonalizedRecommendationGenre(
                        row.genreId(),
                        row.genreName()
                ))
                .distinct()
                .toList();

        List<Long> genreIds = genres.stream()
                .map(PersonalizedRecommendationGenre::id)
                .toList();

        int genreBonus = genreBonusCalculator.applyAsInt(genreIds);

        BigDecimal igdbScore = game.igdbRating() == null
                ? BigDecimal.ZERO
                : game.igdbRating().multiply(IGDB_RATING_WEIGHT);

        BigDecimal recommendationScore = BigDecimal
                .valueOf(relatedScore + genreBonus)
                .add(igdbScore);

        return new PersonalizedRecommendationResult(
                gameId,
                game.title(),
                game.coverImageUrl(),
                game.igdbRating(),
                game.averageRating(),
                game.releaseDate(),
                recommendationScore,
                genres
        );
    }

    // 기록 기반 추천의 1순위 장르에 10점, 2순위 장르에 5점 부여
    private int calculateRecordGenreBonus(
            List<PersonalizedGenrePreference> topGenres,
            List<Long> candidateGenreIds
    ) {
        int genreBonus = 0;

        if (!topGenres.isEmpty()
                && candidateGenreIds.contains(topGenres.get(0).genreId())) {
            genreBonus += 10;
        }

        if (topGenres.size() > 1
                && candidateGenreIds.contains(topGenres.get(1).genreId())) {
            genreBonus += 5;
        }

        return genreBonus;
    }

    // 최종 점수, 사용자 평균 평점, 출시일, 게임 ID 순서로 정렬
    private Comparator<PersonalizedRecommendationResult> resultComparator() {
        return Comparator
                .comparing(
                        PersonalizedRecommendationResult::recommendationScore,
                        Comparator.reverseOrder()
                )
                .thenComparing(
                        PersonalizedRecommendationResult::averageRating,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        PersonalizedRecommendationResult::releaseDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(PersonalizedRecommendationResult::gameId);
    }

}