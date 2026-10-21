package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCalculationRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCandidateRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.support.MySqlTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(MySqlTestConfiguration.class)
@ActiveProfiles("test")
class RelatedGamePrecomputeServiceTest {

    private static final AtomicLong IGDB_IDS = new AtomicLong(1_900_000);
    private static final LocalDateTime PREVIOUS_SUCCESS = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Autowired
    private RelatedGamePrecomputeService service;

    @MockitoSpyBean
    private RelatedGameQueryRepository queryRepository;

    @Autowired
    private RelatedGameCandidateRepository candidateRepository;

    @Autowired
    private RelatedGameCalculationRepository calculationRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> gameIds = new ArrayList<>();
    private final List<Long> genreIds = new ArrayList<>();

    @AfterEach
    void cleanUpCommittedFixtures() {
        inTransaction(() -> {
            if (!gameIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM GameGenre link WHERE link.game.id IN :ids")
                        .setParameter("ids", gameIds).executeUpdate();
                entityManager.createQuery("DELETE FROM Game game WHERE game.id IN :ids")
                        .setParameter("ids", gameIds).executeUpdate();
            }
            if (!genreIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Genre genre WHERE genre.id IN :ids")
                        .setParameter("ids", genreIds).executeUpdate();
            }
            return null;
        });
    }

    @Test
    void initialLoadStoresTopTwentyAndPreservesExistingTopFiveScoresAndOrder() {
        Fixture fixture = createFixture(25, 0);
        List<RelatedGameResponse> existingTopFive = inTransaction(
                () -> queryRepository.findRelatedGames(fixture.sourceId()));

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.INITIAL_LOAD)).isEqualTo(20);

        List<RelatedGameCandidateProjection> saved = storedCandidates(fixture.sourceId());
        assertThat(saved).hasSize(20);
        assertThat(saved.subList(0, 5)).extracting(RelatedGameCandidateProjection::gameId)
                .containsExactlyElementsOf(existingTopFive.stream().map(RelatedGameResponse::id).toList());
        for (int index = 0; index < 5; index++) {
            assertThat(saved.get(index).relatedScore())
                    .isEqualByComparingTo(existingTopFive.get(index).recommendationScore());
        }
        inTransaction(() -> {
            assertThat(candidateRepository.findByIdSourceGameIdOrderByRelatedRankAsc(fixture.sourceId()))
                    .extracting(RelatedGameCandidate::getRelatedRank)
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, 20).boxed().toList());
            RelatedGameCalculation calculation = calculationRepository.findById(fixture.sourceId()).orElseThrow();
            assertThat(calculation.isReady()).isTrue();
            assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.SUCCESS);
            return null;
        });
    }

    @Test
    void refreshUsesOnlyReadyCandidatesButInitialLoadDoesNot() {
        Fixture fixture = createFixture(3, 1);

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.REFRESH)).isEqualTo(1);
        assertThat(storedCandidates(fixture.sourceId())).extracting(RelatedGameCandidateProjection::gameId)
                .containsExactly(fixture.candidateIds().getFirst());

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.INITIAL_LOAD)).isEqualTo(3);
        assertThat(storedCandidates(fixture.sourceId())).hasSize(3);
    }

    @Test
    void sharedBaselineIsSavedWithoutExecutingPerSourceRecommendationQuery() {
        Fixture fixture = createFixture(3, 0);
        RelatedGamePrecomputeSnapshot snapshot = service.prepare(RelatedGamePrecomputeMode.INITIAL_LOAD,
                fixture.candidateIds().getLast());

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.INITIAL_LOAD, snapshot))
                .isEqualTo(3);

        verify(queryRepository, never()).findCandidatesForPrecompute(fixture.sourceId(), 20, false);
        assertThat(storedCandidates(fixture.sourceId())).extracting(RelatedGameCandidateProjection::gameId)
                .containsExactly(fixture.candidateIds().get(2), fixture.candidateIds().get(1),
                        fixture.candidateIds().getFirst());
    }

    @Test
    void interactionSourceStillUsesExistingQueryAndAtomicStore() {
        Fixture fixture = createFixture(3, 0);
        var snapshot = new RelatedGamePrecomputeSnapshot(RelatedGamePrecomputeMode.INITIAL_LOAD, 20,
                Map.of(fixture.sourceId(), List.of(1L)), Map.of(), Set.of(fixture.sourceId()));

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.INITIAL_LOAD, snapshot))
                .isEqualTo(3);

        verify(queryRepository).findCandidatesForPrecompute(fixture.sourceId(), 20, false);
        assertThat(storedCandidates(fixture.sourceId())).hasSize(3);
    }

    @Test
    void replacesExistingPairsAndRanksWithoutLeavingOldScores() {
        Fixture fixture = createFixture(3, 3);
        seedPreviousResult(fixture);

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.REFRESH)).isEqualTo(3);

        List<RelatedGameCandidateProjection> saved = storedCandidates(fixture.sourceId());
        assertThat(saved).extracting(RelatedGameCandidateProjection::gameId)
                .containsExactly(fixture.candidateIds().get(2), fixture.candidateIds().get(1),
                        fixture.candidateIds().getFirst());
        assertThat(saved).allSatisfy(candidate ->
                assertThat(candidate.relatedScore()).isGreaterThan(new BigDecimal("50")));
        assertThat(calculationRepository.findById(fixture.sourceId()).orElseThrow().getLastSuccessAt())
                .isAfter(PREVIOUS_SUCCESS);
    }

    @Test
    void successfulEmptyResultRemovesOldCandidatesAndStaysReady() {
        Fixture fixture = createFixture(1, 1);
        seedPreviousResult(fixture);
        inTransaction(() -> {
            entityManager.createQuery("DELETE FROM GameGenre link WHERE link.game.id = :id")
                    .setParameter("id", fixture.sourceId()).executeUpdate();
            return null;
        });

        assertThat(service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.REFRESH)).isZero();

        assertThat(storedCandidates(fixture.sourceId())).isEmpty();
        RelatedGameCalculation calculation = calculationRepository.findById(fixture.sourceId()).orElseThrow();
        assertThat(calculation.isReady()).isTrue();
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.SUCCESS);
    }

    @Test
    void queryFailurePreservesPreviousCandidatesAndRecordsFailureSeparately() {
        Fixture fixture = createFixture(1, 1);
        seedPreviousResult(fixture);
        doThrow(new DataAccessResourceFailureException("후보 조회 실패")).when(queryRepository)
                .findCandidatesForPrecompute(fixture.sourceId(), 20, true);

        assertThatThrownBy(() -> service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.REFRESH))
                .isInstanceOf(DataAccessResourceFailureException.class).hasMessage("후보 조회 실패");

        assertPreviousResultAndFailedState(fixture);
    }

    @Test
    void databaseInsertFailureRollsBackDeletionAndSuccessStateButCommitsFailureRecord() {
        Fixture fixture = createFixture(1, 1);
        seedPreviousResult(fixture);
        doReturn(List.of(
                new RelatedGameCandidateProjection(fixture.candidateIds().getFirst(), new BigDecimal("99")),
                new RelatedGameCandidateProjection(Long.MAX_VALUE, new BigDecimal("98"))
        )).when(queryRepository).findCandidatesForPrecompute(fixture.sourceId(), 20, true);

        // 기존 목록 삭제와 첫 후보 삽입 후 실제 MySQL 외래키 오류를 발생시킨다.
        assertThatThrownBy(() -> service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.REFRESH))
                .isInstanceOf(PersistenceException.class);

        assertPreviousResultAndFailedState(fixture);
    }

    @Test
    void firstCalculationFailureCreatesFailedUnreadyState() {
        Fixture fixture = createFixture(0, 0);
        doThrow(new DataAccessResourceFailureException("최초 계산 실패")).when(queryRepository)
                .findCandidatesForPrecompute(fixture.sourceId(), 20, false);

        assertThatThrownBy(() -> service.precompute(fixture.sourceId(), RelatedGamePrecomputeMode.INITIAL_LOAD))
                .isInstanceOf(DataAccessResourceFailureException.class);

        RelatedGameCalculation calculation = calculationRepository.findById(fixture.sourceId()).orElseThrow();
        assertThat(calculation.isReady()).isFalse();
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);
        assertThat(calculation.getLastError()).contains("최초 계산 실패");
    }

    @Test
    void unknownSourceDoesNotCreateCalculationState() {
        assertThatThrownBy(() -> service.precompute(Long.MAX_VALUE, RelatedGamePrecomputeMode.INITIAL_LOAD))
                .isInstanceOf(ServiceException.class)
                .satisfies(error -> assertThat(((ServiceException) error).getResultCode()).isEqualTo("404-1"));
        assertThat(calculationRepository.existsById(Long.MAX_VALUE)).isFalse();
    }

    private void assertPreviousResultAndFailedState(Fixture fixture) {
        assertThat(storedCandidates(fixture.sourceId())).containsExactly(
                new RelatedGameCandidateProjection(fixture.candidateIds().getFirst(), new BigDecimal("1.0000")));
        RelatedGameCalculation calculation = calculationRepository.findById(fixture.sourceId()).orElseThrow();
        assertThat(calculation.isReady()).isTrue();
        assertThat(calculation.getLastSuccessAt()).isEqualTo(PREVIOUS_SUCCESS);
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);
        assertThat(calculation.getLastError()).isNotBlank();
    }

    private void seedPreviousResult(Fixture fixture) {
        inTransaction(() -> {
            Game source = entityManager.getReference(Game.class, fixture.sourceId());
            RelatedGameCalculation calculation = new RelatedGameCalculation(source);
            calculation.start(PREVIOUS_SUCCESS.minusMinutes(1));
            calculation.succeed(PREVIOUS_SUCCESS);
            entityManager.persist(calculation);
            entityManager.persist(new RelatedGameCandidate(source,
                    entityManager.getReference(Game.class, fixture.candidateIds().getFirst()), BigDecimal.ONE, 1));
            return null;
        });
    }

    private List<RelatedGameCandidateProjection> storedCandidates(Long sourceId) {
        return inTransaction(() -> candidateRepository.findByIdSourceGameIdOrderByRelatedRankAsc(sourceId).stream()
                .map(candidate -> new RelatedGameCandidateProjection(
                        candidate.getId().getCandidateGameId(), candidate.getRelatedScore()))
                .toList());
    }

    private Fixture createFixture(int candidateCount, int readyCount) {
        return inTransaction(() -> {
            long igdbId = IGDB_IDS.getAndAdd(100);
            Genre genre = Genre.createFromIgdb(igdbId, "Precompute " + igdbId);
            entityManager.persist(genre);
            genreIds.add(genre.getId());
            Game source = saveGame(igdbId, new BigDecimal("90.00"), genre);
            List<Long> candidates = new ArrayList<>();
            for (int index = 0; index < candidateCount; index++) {
                BigDecimal rating = new BigDecimal("80.00").add(new BigDecimal("0.10").multiply(BigDecimal.valueOf(index)));
                Game candidate = saveGame(igdbId + index + 1, rating, genre);
                candidates.add(candidate.getId());
                if (index < readyCount) {
                    RelatedGameCalculation calculation = new RelatedGameCalculation(candidate);
                    calculation.start(PREVIOUS_SUCCESS.minusMinutes(1));
                    calculation.succeed(PREVIOUS_SUCCESS);
                    entityManager.persist(calculation);
                }
            }
            return new Fixture(source.getId(), List.copyOf(candidates));
        });
    }

    private Game saveGame(Long igdbId, BigDecimal rating, Genre genre) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(igdbId, "Precompute " + igdbId,
                null, null, 1704067200L, rating, List.of(), List.of(), List.of(), List.of()));
        entityManager.persist(game);
        gameIds.add(game.getId());
        entityManager.persist(new GameGenre(game, genre));
        return game;
    }

    private <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private record Fixture(Long sourceId, List<Long> candidateIds) {
    }
}
