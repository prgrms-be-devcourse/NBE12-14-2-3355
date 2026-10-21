package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.RelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGamePrecomputeSnapshotRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RelatedGamePrecomputeService {

    // 사용자별 제외 후 상위 5개를 확보하기 위해 기준 게임별 연관 후보를 최대 20개 저장 (이후 변경 가능)
    private static final int CANDIDATE_LIMIT = 20;

    private final RelatedGameQueryRepository queryRepository;
    private final RelatedGamePrecomputeStore store;
    private final RelatedGamePrecomputeSnapshotRepository snapshotRepository;

    public RelatedGamePrecomputeSnapshot prepare(RelatedGamePrecomputeMode mode, long upperId) {
        return snapshotRepository.load(Objects.requireNonNull(mode, "mode"), upperId, CANDIDATE_LIMIT);
    }

    // 외부 실행은 단일 실행 잠금을 담당하는 배치 서비스를 통해 호출한다.
    public int precompute(Long gameId, RelatedGamePrecomputeMode mode) {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(mode, "mode");
        return calculateAndReplace(gameId, () -> queryRepository.findCandidatesForPrecompute(
                gameId, CANDIDATE_LIMIT, mode == RelatedGamePrecomputeMode.REFRESH));
    }

    public int precompute(Long gameId, RelatedGamePrecomputeMode mode, RelatedGamePrecomputeSnapshot snapshot) {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.mode() != Objects.requireNonNull(mode, "mode")) {
            throw new IllegalArgumentException("계산 모드와 공통 데이터의 모드가 다릅니다.");
        }
        return calculateAndReplace(gameId, () -> snapshot.baselineFor(gameId).orElseGet(
                () -> queryRepository.findCandidatesForPrecompute(
                        gameId, CANDIDATE_LIMIT, mode == RelatedGamePrecomputeMode.REFRESH)));
    }

    private int calculateAndReplace(Long gameId, Supplier<List<RelatedGameCandidateProjection>> calculate) {
        LocalDateTime attemptedAt = LocalDateTime.now();
        store.start(gameId, attemptedAt);
        try {
            List<RelatedGameCandidateProjection> candidates = calculate.get();
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
