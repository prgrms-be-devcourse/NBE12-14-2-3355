package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.*;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.sync.IgdbSyncCatalog;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "recommendation.publication.ready-only=true",
        "recommendation.related.precomputed-enabled=true",
        "recommendation.personalized.precomputed-enabled=true"
})
@MySqlIntegrationTest
class RecommendationLifecycleTest {
    private static final LocalDateTime PREVIOUS_SUCCESS = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired GameImportService imports;
    @Autowired GameRepository games;
    @Autowired RelatedGameCalculationRepository calculations;
    @Autowired RelatedGameCandidateRepository candidates;
    @Autowired RelatedGamePrecomputeBatchService batch;
    @Autowired RelatedGamePrecomputeLock lock;
    @Autowired GameDetailService details;
    @Autowired GameService catalog;
    @Autowired IgdbSyncCatalog syncCatalog;
    @Autowired PersonalizedGameRecommendationService personalized;
    @MockitoSpyBean RelatedGameQueryRepository query;
    @MockitoSpyBean RelatedGameCandidateBatchRepository writer;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
    }

    @AfterEach
    void cleanUp() {
        tx.executeWithoutResult(status -> {
            em.createQuery("delete from UserGame").executeUpdate();
            em.createQuery("delete from RelatedGameCandidate").executeUpdate();
            em.createQuery("delete from RelatedGameCalculation").executeUpdate();
            em.createQuery("delete from GameGenre").executeUpdate();
            em.createQuery("delete from Game").executeUpdate();
            em.createQuery("delete from Genre").executeUpdate();
            em.createQuery("delete from User").executeUpdate();
        });
    }

    @Test
    void failedNewGameStaysHiddenUntilManualRetrySucceeds() {
        Long candidateId = tx.execute(status -> readyGame(100L, "기존 후보").getId());
        doThrow(new IllegalStateException("계산 오류"))
                .when(query).findCandidatesForPrecompute(anyLong(), eq(20), eq(true));

        imports.saveGames(List.of(response(200L, "신규 게임")));
        Long sourceId = games.findByIgdbId(200L).orElseThrow().getId();
        var failed = calculations.findById(sourceId).orElseThrow();
        assertThat(failed.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);
        assertThat(failed.getLastSuccessAt()).isNull();
        assertHidden(sourceId, "신규 게임");

        doCallRealMethod().when(query).findCandidatesForPrecompute(anyLong(), eq(20), eq(true));
        var retry = batch.run(false, List.of(sourceId), RelatedGamePrecomputeMode.REFRESH);

        assertThat(retry.succeeded()).isEqualTo(1);
        assertThat(retry.failedIds()).isEmpty();
        assertThat(calculations.findById(sourceId).orElseThrow().isReady()).isTrue();
        assertThat(details.getRelatedGames(sourceId)).extracting(RelatedGameResponse::id)
                .containsExactly(candidateId);
        assertThat(catalog.getSuggestions("신규 게임")).extracting(GameListResponse::id)
                .containsExactly(sourceId);
    }

    @Test
    void failureAfterJdbcInsertRollsBackReplacementAndKeepsGamePublicWhileBatchContinues() {
        List<Long> ids = tx.execute(status -> {
            Game source = readyGame(100L, "기존 기준");
            Game previous = readyGame(200L, "이전 후보");
            Game next = readyGame(300L, "다음 기준");
            em.persist(new RelatedGameCandidate(source, previous, new BigDecimal("77.1234"), 1));
            return List.of(source.getId(), previous.getId(), next.getId());
        });
        RelatedGameCandidateBatchRepository writerTarget = AopTestUtils.getUltimateTargetObject(writer);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("INSERT 후 오류");
        }).when(writerTarget).insertAll(eq(ids.getFirst()), anyList());

        var result = batch.run(false, List.of(ids.getFirst(), ids.getLast()), RelatedGamePrecomputeMode.REFRESH);

        assertThat(result.failedIds()).containsExactly(ids.getFirst());
        assertThat(result.succeeded()).isEqualTo(1);
        var state = calculations.findById(ids.getFirst()).orElseThrow();
        assertThat(state.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);
        assertThat(state.getLastSuccessAt()).isEqualTo(PREVIOUS_SUCCESS);
        var related = details.getRelatedGames(ids.getFirst());
        assertThat(related).extracting(RelatedGameResponse::id).containsExactly(ids.get(1));
        assertThat(related.getFirst().recommendationScore()).isEqualByComparingTo("77.1234");
        assertThat(details.getGameDetail(ids.getFirst()).id()).isEqualTo(ids.getFirst());
        assertThat(calculations.findById(ids.getLast()).orElseThrow().getLastAttemptStatus())
                .isEqualTo(RelatedGameCalculationStatus.SUCCESS);
    }

    @Test
    void realLockContentionPreservesNewPendingGameUntilRetry() {
        tx.executeWithoutResult(status -> readyGame(100L, "기존 후보"));
        Long sourceId = lock.tryRun(checkOwnership -> {
            imports.saveGames(List.of(response(200L, "대기 게임")));
            Long id = games.findByIgdbId(200L).orElseThrow().getId();
            var state = calculations.findById(id).orElseThrow();
            assertThat(state.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.PENDING);
            assertThat(state.getLastSuccessAt()).isNull();
            assertHidden(id, "대기 게임");
            checkOwnership.run();
            return id;
        }).orElseThrow();

        assertThat(batch.run(false, List.of(sourceId), RelatedGamePrecomputeMode.REFRESH).exitCode()).isZero();
        assertThat(details.getRelatedGames(sourceId)).hasSize(1);
        assertThat(calculations.findById(sourceId).orElseThrow().isReady()).isTrue();
    }

    @Test
    void syncDeletionCascadesBothCandidateDirectionsAndCalculationWithoutDeletingSurvivors() {
        List<Long> ids = tx.execute(status -> {
            Game removed = readyGame(100L, "삭제 대상");
            Game first = readyGame(200L, "남을 기준");
            Game second = readyGame(300L, "남을 후보");
            em.persist(new RelatedGameCandidate(removed, first, BigDecimal.TEN, 1));
            em.persist(new RelatedGameCandidate(first, removed, BigDecimal.TEN, 1));
            em.persist(new RelatedGameCandidate(first, second, BigDecimal.ONE, 2));
            return List.of(removed.getId(), first.getId(), second.getId());
        });

        tx.executeWithoutResult(status -> {
            var ineligible = new IgdbGameResponse(100L, "삭제 대상", null, null, null,
                    null, List.of(), List.of(), List.of(), List.of());
            assertThat(syncCatalog.apply(List.of(ineligible), Instant.now().getEpochSecond()).deleted()).isEqualTo(1);
        });

        assertThat(games.existsById(ids.getFirst())).isFalse();
        assertThat(calculations.existsById(ids.getFirst())).isFalse();
        assertThat(candidates.count()).isEqualTo(1);
        assertThat(details.getRelatedGames(ids.get(1))).extracting(RelatedGameResponse::id)
                .containsExactly(ids.getLast());
        assertThat(calculations.findById(ids.get(1)).orElseThrow().isReady()).isTrue();
    }

    @Test
    void personalizedFallbackExcludesRecordedAndUnreadyGamesAndKeepsRankScoring() {
        var fixture = tx.execute(status -> {
            Game source = readyGame(100L, "기록 기준");
            List<Game> pool = IntStream.rangeClosed(1, 6)
                    .mapToObj(i -> readyGame(200L + i, "후보 " + i)).toList();
            game(999L, "미준비 후보");
            em.persist(new RelatedGameCandidate(source, pool.get(0), new BigDecimal("999"), 1));
            em.persist(new RelatedGameCandidate(source, pool.get(1), new BigDecimal("888"), 2));
            User user = new User("boundary", "boundary@test.com", "password");
            em.persist(user);
            record(user, source, PlayStatus.COMPLETED);
            record(user, pool.getFirst(), PlayStatus.DROPPED);
            return new PersonalizedFixture(user.getId(), pool.subList(1, 6).stream().map(Game::getId).toList());
        });

        var result = personalized.recommend(fixture.userId());

        assertThat(result).extracting(PersonalizedGameRecommendationResponse::id)
                .containsExactlyElementsOf(fixture.expectedIds());
        assertThat(result.get(0).recommendationScore()).isEqualByComparingTo("70");
        assertThat(result.get(1).recommendationScore()).isEqualByComparingTo("67");
    }

    private void assertHidden(Long id, String title) {
        assertThatThrownBy(() -> details.getGameDetail(id)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> details.getRelatedGames(id)).isInstanceOf(ServiceException.class);
        assertThat(catalog.getSuggestions(title)).isEmpty();
    }

    private Game readyGame(long igdbId, String title) {
        Game game = game(igdbId, title);
        var state = new RelatedGameCalculation(game);
        state.start(PREVIOUS_SUCCESS);
        state.succeed(PREVIOUS_SUCCESS);
        em.persist(state);
        return game;
    }

    private Game game(long igdbId, String title) {
        Game game = Game.createFromIgdb(response(igdbId, title));
        em.persist(game);
        Genre genre = em.createQuery("select genre from Genre genre where genre.igdbGenreId = 10", Genre.class)
                .getResultStream().findFirst().orElseGet(() -> {
                    Genre created = Genre.createFromIgdb(10L, "RPG");
                    em.persist(created);
                    return created;
                });
        em.persist(new GameGenre(game, genre));
        return game;
    }

    private IgdbGameResponse response(long igdbId, String title) {
        return new IgdbGameResponse(igdbId, title, null, null, 0L, new BigDecimal("80"), List.of(),
                List.of(new IgdbGameResponse.NamedResource(10L, "RPG")), List.of(), List.of());
    }

    private void record(User user, Game game, PlayStatus status) {
        UserGame record = new UserGame(user, game);
        record.changePlayStatus(status);
        em.persist(record);
    }

    private record PersonalizedFixture(Long userId, List<Long> expectedIds) {}
}
