package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedGamePrecomputeSnapshotTest {

    @Test
    void reusesOneGenreRankingAndRetainsTwentyAfterSelfExclusion() {
        Map<Long, List<Long>> genres = new HashMap<>();
        Map<Long, BigDecimal> ratings = new HashMap<>();
        LongStream.rangeClosed(1, 25).forEach(id -> {
            genres.put(id, id % 2 == 0 ? List.of(2L, 1L, 1L) : List.of(1L, 2L));
            ratings.put(id, new BigDecimal("80.00"));
        });
        var snapshot = snapshot(genres, ratings, Set.of());

        assertThat(ids(snapshot, 1L)).containsExactlyElementsOf(LongStream.rangeClosed(2, 21).boxed().toList());
        assertThat(ids(snapshot, 2L)).containsExactlyElementsOf(LongStream.rangeClosed(1, 21)
                .filter(id -> id != 2).boxed().toList());
        assertThat(ids(snapshot, 25L)).containsExactlyElementsOf(LongStream.rangeClosed(1, 20).boxed().toList());
        assertThat(snapshot.calculatedGroupCount()).isEqualTo(1);
    }

    @Test
    void separatesPartiallyMatchingGenresAndPreservesFractionalScore() {
        var snapshot = snapshot(Map.of(
                1L, List.of(1L, 2L), 2L, List.of(1L),
                3L, List.of(1L, 2L), 4L, List.of(3L)),
                Map.of(3L, new BigDecimal("80.25"), 4L, new BigDecimal("99.00")), Set.of());

        assertThat(snapshot.baselineFor(1L).orElseThrow().getFirst().relatedScore())
                .isEqualByComparingTo("70.125");
        assertThat(snapshot.baselineFor(2L).orElseThrow().getFirst().relatedScore())
                .isEqualByComparingTo("55.125");
        assertThat(snapshot.calculatedGroupCount()).isEqualTo(2);
        assertThat(ids(snapshot, 1L)).containsExactly(3L);
    }

    @Test
    void supportsMoreThanSixtyFourDistinctGenresWithoutBitCollisions() {
        Map<Long, List<Long>> genres = new HashMap<>();
        LongStream.rangeClosed(1, 70).forEach(id -> genres.put(id, List.of(id)));
        genres.put(100L, List.of(1L, 65L, 70L));
        var snapshot = snapshot(genres, Map.of(1L, new BigDecimal("80"),
                2L, new BigDecimal("99"), 65L, new BigDecimal("80"), 70L, new BigDecimal("80")), Set.of());

        assertThat(ids(snapshot, 100L)).containsExactly(1L, 65L, 70L);
    }

    @Test
    void distinguishesEmptyResultFromSourcesThatNeedExistingQuery() {
        var snapshot = snapshot(Map.of(1L, List.of(1L), 2L, List.of()), Map.of(), Set.of(1L));

        assertThat(snapshot.baselineFor(1L)).isEmpty();
        assertThat(snapshot.baselineFor(999L)).isEmpty();
        assertThat(snapshot.baselineFor(2L)).isPresent().contains(List.of());
    }

    private RelatedGamePrecomputeSnapshot snapshot(Map<Long, List<Long>> genres,
                                                  Map<Long, BigDecimal> ratings, Set<Long> interactions) {
        return new RelatedGamePrecomputeSnapshot(RelatedGamePrecomputeMode.INITIAL_LOAD, 20,
                genres, ratings, interactions);
    }

    private List<Long> ids(RelatedGamePrecomputeSnapshot snapshot, Long sourceId) {
        return snapshot.baselineFor(sourceId).orElseThrow().stream()
                .map(RelatedGameCandidateProjection::gameId).toList();
    }
}
