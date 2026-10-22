package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER",
        "recommendation.related.precomputed-enabled=true"
})
@ActiveProfiles("test")
@Transactional
class PrecomputedRelatedGameRecommendationTest {

    @Autowired
    private GameDetailService service;
    @Autowired
    private EntityManager em;

    private Game source;
    private Genre genre;

    @BeforeEach
    void setUp() {
        source = saveGame(9000L, "기준 게임", "90");
        genre = Genre.createFromIgdb(9000L, "RPG");
        em.persist(genre);
        em.persist(new GameGenre(source, genre));
    }

    @Test
    void returnsFiveByStoredRankWithStoredScoresAndCurrentGameInformation() {
        calculation(true, RelatedGameCalculationStatus.SUCCESS);
        // 현재 평점이 후보 조건에 미달해도 다음 갱신까지 저장된 자격·순위를 유지한다.
        List<Game> candidates = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> saveGame(9000L + i, "후보 " + i, "60"))
                .toList();
        for (int i = 5; i >= 0; i--) {
            em.persist(new RelatedGameCandidate(source, candidates.get(i),
                    new BigDecimal("10.1234").add(BigDecimal.valueOf(i)), i + 1));
            em.persist(new GameGenre(candidates.get(i), genre));
        }
        em.flush();
        em.clear();

        var result = service.getRelatedGames(source.getId());

        assertThat(result).extracting(RelatedGameResponse::id)
                .containsExactlyElementsOf(candidates.subList(0, 5).stream().map(Game::getId).toList());
        assertThat(result.getFirst().recommendationScore()).isEqualByComparingTo("10.1234");
        assertThat(result.getFirst().title()).isEqualTo("후보 1");
        assertThat(result.getFirst().igdbRating()).isEqualByComparingTo("60");
        assertThat(result.getFirst().genres()).extracting(GameDetailResponse.GenreResponse::name)
                .containsExactly("RPG");
    }

    @Test
    void completedEmptyResultDoesNotFallBackToRealtimeCandidates() {
        calculation(true, RelatedGameCalculationStatus.SUCCESS);
        saveEligibleCandidate();
        em.flush();

        assertThat(service.getRelatedGames(source.getId())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = RelatedGameCalculationStatus.class, names = {"SUCCESS", "RUNNING", "FAILED"})
    void previousSuccessRemainsReadableDuringRefreshOrAfterFailure(RelatedGameCalculationStatus status) {
        calculation(true, status);
        Game candidate = saveGame(9010L, "이전 정상 후보", "60");
        em.persist(new RelatedGameCandidate(source, candidate, new BigDecimal("77.5"), 1));
        em.flush();

        assertThat(service.getRelatedGames(source.getId())).extracting(RelatedGameResponse::id)
                .containsExactly(candidate.getId());
    }

    @Test
    void missingCalculationUsesExistingQuery() {
        Game candidate = saveEligibleCandidate();
        em.flush();

        assertThat(service.getRelatedGames(source.getId())).extracting(RelatedGameResponse::id)
                .containsExactly(candidate.getId());
    }

    @ParameterizedTest
    @EnumSource(value = RelatedGameCalculationStatus.class, names = {"PENDING", "RUNNING", "FAILED"})
    void calculationWithoutSuccessUsesExistingQuery(RelatedGameCalculationStatus status) {
        calculation(false, status);
        Game candidate = saveEligibleCandidate();
        em.flush();

        assertThat(service.getRelatedGames(source.getId())).extracting(RelatedGameResponse::id)
                .containsExactly(candidate.getId());
    }

    @Test
    void missingGamePreservesExistingError() {
        assertThatThrownBy(() -> service.getRelatedGames(Long.MAX_VALUE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("존재하지 않는 게임입니다.");
    }

    private void calculation(boolean previouslySucceeded, RelatedGameCalculationStatus status) {
        var calculation = new RelatedGameCalculation(source);
        var now = LocalDateTime.now();
        if (previouslySucceeded) {
            calculation.start(now.minusDays(1));
            calculation.succeed(now.minusDays(1));
        }
        if (status == RelatedGameCalculationStatus.RUNNING) calculation.start(now);
        if (status == RelatedGameCalculationStatus.FAILED) calculation.fail(now, "갱신 실패");
        em.persist(calculation);
    }

    private Game saveEligibleCandidate() {
        Game candidate = saveGame(9020L, "실시간 후보", "80");
        em.persist(new GameGenre(candidate, genre));
        return candidate;
    }

    private Game saveGame(long igdbId, String title, String rating) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(igdbId, title, null, null, null,
                new BigDecimal(rating), List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        return game;
    }
}
