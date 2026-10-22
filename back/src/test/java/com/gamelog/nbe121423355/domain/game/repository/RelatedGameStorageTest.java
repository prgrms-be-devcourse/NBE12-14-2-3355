package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.support.MySqlTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(MySqlTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class RelatedGameStorageTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RelatedGameCandidateRepository candidateRepository;

    @Autowired
    private RelatedGameCalculationRepository calculationRepository;

    @Test
    void retrievesCandidatesByRankAndPreservesScorePrecision() {
        Game source = saveGame(71001L);
        Game first = saveGame(71002L);
        Game second = saveGame(71003L);
        candidateRepository.saveAll(List.of(
                new RelatedGameCandidate(source, second, new BigDecimal("80.1250"), 2),
                new RelatedGameCandidate(source, first, new BigDecimal("90.5550"), 1)
        ));
        entityManager.flush();
        entityManager.clear();

        var candidates = candidateRepository.findByIdSourceGameIdOrderByRelatedRankAsc(source.getId());

        assertThat(candidates).extracting(candidate -> candidate.getId().getCandidateGameId())
                .containsExactly(first.getId(), second.getId());
        assertThat(candidates.getFirst().getRelatedScore()).isEqualByComparingTo("90.5550");
    }

    @Test
    void rejectsDuplicateCandidatePairInDatabase() {
        Game source = saveGame(72001L);
        Game target = saveGame(72002L);
        persistCandidate(source, target, 1);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> {
            persistCandidate(entityManager.getReference(Game.class, source.getId()),
                    entityManager.getReference(Game.class, target.getId()), 2);
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void rejectsDuplicateRankWithinSourceInDatabase() {
        Game source = saveGame(73001L);
        persistCandidate(source, saveGame(73002L), 1);
        persistCandidate(source, saveGame(73003L), 1);

        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(PersistenceException.class);
    }

    @Test
    void deletesOnlyRequestedSourcesCandidates() {
        Game source = saveGame(74001L);
        Game otherSource = saveGame(74002L);
        Game target = saveGame(74003L);
        persistCandidate(source, target, 1);
        persistCandidate(otherSource, target, 1);
        entityManager.flush();

        assertThat(candidateRepository.deleteAllBySourceGameId(source.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(candidateRepository.findByIdSourceGameIdOrderByRelatedRankAsc(source.getId())).isEmpty();
        assertThat(candidateRepository.findByIdSourceGameIdOrderByRelatedRankAsc(otherSource.getId())).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nativeGameDeletionCascadesBothCandidateDirectionsAndCalculation(boolean deleteSource) {
        Game source = saveGame(75001L);
        Game target = saveGame(75002L);
        persistCandidate(source, target, 1);
        calculationRepository.save(new RelatedGameCalculation(source));
        calculationRepository.save(new RelatedGameCalculation(target));
        entityManager.flush();
        entityManager.clear();
        Long deletedId = deleteSource ? source.getId() : target.getId();
        Long retainedId = deleteSource ? target.getId() : source.getId();

        // IGDB 동기화처럼 JPA 삭제 훅을 거치지 않는 경로도 검증한다.
        entityManager.createNativeQuery("DELETE FROM games WHERE id = :gameId")
                .setParameter("gameId", deletedId).executeUpdate();
        entityManager.clear();

        assertThat(candidateRepository.existsById(new RelatedGameCandidateId(source.getId(), target.getId())))
                .isFalse();
        assertThat(calculationRepository.existsById(deletedId)).isFalse();
        assertThat(calculationRepository.existsById(retainedId)).isTrue();
    }

    @Test
    void failedRefreshStaysReadyAndInitialFailuresRemainInPagedUnreadyQuery() {
        Game ready = saveGame(76001L);
        Game failed = saveGame(76002L);
        Game pending = saveGame(76003L);
        LocalDateTime attemptedAt = LocalDateTime.of(2026, 10, 21, 10, 0);
        RelatedGameCalculation readyCalculation = new RelatedGameCalculation(ready);
        readyCalculation.start(attemptedAt);
        readyCalculation.succeed(attemptedAt.plusMinutes(1));
        readyCalculation.fail(attemptedAt.plusDays(1), "갱신 실패");
        RelatedGameCalculation failedCalculation = new RelatedGameCalculation(failed);
        failedCalculation.fail(attemptedAt, "최초 계산 실패");
        calculationRepository.saveAll(List.of(readyCalculation, failedCalculation,
                new RelatedGameCalculation(pending)));
        entityManager.flush();
        entityManager.clear();

        assertThat(calculationRepository.existsByGameIdAndLastSuccessAtIsNotNull(ready.getId())).isTrue();
        assertThat(calculationRepository.existsByGameIdAndLastSuccessAtIsNotNull(failed.getId())).isFalse();
        assertThat(calculationRepository.findUnreadyGameIdsAfter(0L, PageRequest.of(0, 1)))
                .containsExactly(failed.getId());
        assertThat(calculationRepository.findUnreadyGameIdsAfter(failed.getId(), PageRequest.of(0, 1)))
                .containsExactly(pending.getId());
    }

    private void persistCandidate(Game source, Game target, int rank) {
        entityManager.persist(new RelatedGameCandidate(source, target, new BigDecimal("90.1250"), rank));
    }

    private Game saveGame(Long igdbId) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(igdbId, "Game " + igdbId,
                null, null, 1704067200L, new BigDecimal("80.25"),
                List.of(), List.of(), List.of(), List.of()));
        entityManager.persist(game);
        return game;
    }
}
