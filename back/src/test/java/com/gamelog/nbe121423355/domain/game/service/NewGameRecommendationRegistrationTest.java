package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCalculationStatus;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCalculationRepository;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.sync.IgdbSyncStore;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "recommendation.publication.ready-only=true")
@ActiveProfiles("test")
class NewGameRecommendationRegistrationTest {
    @Autowired GameImportService imports;
    @Autowired IgdbSyncStore sync;
    @Autowired GameRepository games;
    @Autowired RelatedGameCalculationRepository calculations;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EntityManager em;
    @MockitoBean RelatedGamePrecomputeBatchService batch;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
    }

    @AfterEach
    void cleanUp() {
        tx.executeWithoutResult(status -> {
            em.createQuery("delete from RelatedGameCalculation").executeUpdate();
            em.createQuery("delete from GameGenre").executeUpdate();
            em.createQuery("delete from Game").executeUpdate();
            em.createQuery("delete from Genre").executeUpdate();
            em.createQuery("delete from IgdbSyncState").executeUpdate();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void commitsGameRelationsAndPendingStateBeforeAttemptingCalculation(boolean useSync) {
        when(batch.run(eq(false), anyList(), eq(RelatedGamePrecomputeMode.REFRESH)))
                .thenReturn(new RelatedGamePrecomputeBatchService.Result(false, 0, 0, List.of(), Duration.ZERO));
        Long id = tx.execute(status -> {
            add(useSync);
            var game = games.findByIgdbId(9700L).orElseThrow();
            var state = calculations.findById(game.getId()).orElseThrow();
            assertThat(state.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.PENDING);
            verifyNoInteractions(batch);
            return game.getId();
        });

        verify(batch).run(false, List.of(id), RelatedGamePrecomputeMode.REFRESH);
        // 잠금 경합으로 계산을 시작하지 못해도 커밋된 대기 상태와 장르는 유지된다.
        tx.executeWithoutResult(status -> {
            assertThat(calculations.findById(id).orElseThrow().isReady()).isFalse();
            assertThat(em.createQuery("select count(link) from GameGenre link where link.game.id = :id", Long.class)
                    .setParameter("id", id).getSingleResult()).isEqualTo(1);
        });

        clearInvocations(batch);
        tx.executeWithoutResult(status -> add(useSync));
        verifyNoInteractions(batch); // 기존 게임 업데이트는 신규 상태 생성·즉시 갱신 대상이 아니다.
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rollbackDoesNotLeavePendingStateOrRunCalculation(boolean useSync) {
        tx.executeWithoutResult(status -> {
            add(useSync);
            status.setRollbackOnly();
        });
        assertThat(games.findByIgdbId(9700L)).isEmpty();
        assertThat(calculations.count()).isZero();
        verifyNoInteractions(batch);
    }

    private void add(boolean useSync) {
        var response = new IgdbGameResponse(9700L, "신규 게임", null, null, 0L, new java.math.BigDecimal("80"),
                List.of(), List.of(new IgdbGameResponse.NamedResource(9700L, "RPG")), List.of(), List.of());
        if (useSync) {
            var state = sync.claim(1L);
            sync.savePage(state.getRunId(), List.of(response));
            sync.savePage(state.getRunId(), List.of());
        } else {
            imports.saveGames(List.of(response));
        }
    }
}
