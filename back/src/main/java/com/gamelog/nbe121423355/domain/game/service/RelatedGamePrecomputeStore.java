package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCalculation;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCalculationRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCandidateRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCandidateBatchRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class RelatedGamePrecomputeStore {

    private final EntityManager entityManager;
    private final GameRepository gameRepository;
    private final RelatedGameCalculationRepository calculationRepository;
    private final RelatedGameCandidateRepository candidateRepository;
    private final RelatedGameCandidateBatchRepository candidateBatchRepository;

    public void start(Long gameId, LocalDateTime attemptedAt) {
        if (!gameRepository.existsById(gameId)) {
            throw new ServiceException("404-1", "존재하지 않는 게임입니다.");
        }
        RelatedGameCalculation calculation = calculationRepository.findById(gameId)
                .orElseGet(() -> calculationRepository.save(new RelatedGameCalculation(
                        entityManager.getReference(Game.class, gameId))));
        calculation.start(attemptedAt);
    }

    public void replace(Long gameId, List<RelatedGameCandidateProjection> candidates,
                        LocalDateTime completedAt) {
        RelatedGameCalculation calculation = calculationRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("계산 시작 상태가 없습니다."));

        // 이 메서드는 별도 영속성 컨텍스트에서 실행하므로 이전 후보 엔티티를 로딩하지 않는다.
        candidateRepository.deleteAllBySourceGameId(gameId);
        candidateBatchRepository.insertAll(gameId, candidates);
        calculation.succeed(completedAt);
        // JDBC 저장과 JPA 성공 상태 변경이 같은 연결·트랜잭션에서 반영된다.
        entityManager.flush();
    }

    public void fail(Long gameId, LocalDateTime attemptedAt, String error) {
        // 계산 중 실제 삭제된 게임에는 외래키를 가진 상태를 다시 생성하지 않는다.
        if (!gameRepository.existsById(gameId)) {
            return;
        }
        RelatedGameCalculation calculation = calculationRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("계산 시작 상태가 없습니다."));
        calculation.fail(attemptedAt, error);
    }
}
