package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.RelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RelatedGamePrecomputeService {

    // 사용자별 제외 후 상위 5개를 확보하기 위해 기준 게임별 연관 후보를 최대 20개 저장 (이후 변경 가능)
    private static final int CANDIDATE_LIMIT = 20;

    private final RelatedGameQueryRepository queryRepository;
    private final RelatedGamePrecomputeStore store;

    // 전체 실행과 프로세스 간 중복 실행 제어는 다음 단계의 실행부에서 담당한다.
    public int precompute(Long gameId, RelatedGamePrecomputeMode mode) {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(mode, "mode");
        LocalDateTime attemptedAt = LocalDateTime.now();
        store.start(gameId, attemptedAt);
        try {
            List<RelatedGameCandidateProjection> candidates = queryRepository.findCandidatesForPrecompute(
                    gameId, CANDIDATE_LIMIT, mode == RelatedGamePrecomputeMode.REFRESH
            );
            store.replace(gameId, candidates, LocalDateTime.now());
            return candidates.size();
        } catch (RuntimeException failure) {
            try {
                store.fail(gameId, attemptedAt, failure.getClass().getSimpleName() + ": " + failure.getMessage());
            } catch (RuntimeException recordingFailure) {
                // 실패 기록도 불가능한 경우 원래 계산·저장 예외를 잃지 않는다.
                failure.addSuppressed(recordingFailure);
            }
            throw failure;
        }
    }
}
