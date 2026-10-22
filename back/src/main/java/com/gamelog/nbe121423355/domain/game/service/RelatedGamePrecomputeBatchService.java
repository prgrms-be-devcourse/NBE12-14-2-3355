package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RelatedGamePrecomputeBatchService {

    private static final int PAGE_SIZE = 100;

    private final GameRepository gameRepository;
    private final RelatedGamePrecomputeService precomputeService;
    private final RelatedGamePrecomputeLock executionLock;

    public Result run(boolean all, List<Long> gameIds, RelatedGamePrecomputeMode mode) {
        Objects.requireNonNull(gameIds, "gameIds");
        Objects.requireNonNull(mode, "mode");
        if (all == !gameIds.isEmpty()) {
            throw new IllegalArgumentException("전체 실행과 지정 ID 중 하나를 선택해야 합니다.");
        }
        List<Long> targets = gameIds.stream().map(id -> {
            if (id == null || id < 1) {
                throw new IllegalArgumentException("게임 ID는 양수여야 합니다.");
            }
            return id;
        }).distinct().sorted().toList();
        long startedAt = System.nanoTime();
        return executionLock.tryRun(checkOwnership -> {
            Progress progress = new Progress();
            if (all) {
                // 실행 도중 추가되는 게임 때문에 전체 작업이 계속 늘어나지 않게 경계를 고정한다.
                long upperId = gameRepository.findMaxGameIdForPrecompute();
                long preparationStartedAt = System.nanoTime();
                RelatedGamePrecomputeSnapshot snapshot = precomputeService.prepare(mode, upperId);
                log.info("추천 공통 데이터 준비: 기준 게임={}, 적격 후보={}, 행동 기준 게임={}, 소요시간={}ms",
                        snapshot.sourceCount(), snapshot.candidateCount(), snapshot.interactionSourceCount(),
                        Duration.ofNanos(System.nanoTime() - preparationStartedAt).toMillis());
                long afterId = 0;
                while (true) {
                    checkOwnership.run();
                    List<Long> page = gameRepository.findGameIdsForPrecompute(
                            afterId, upperId, PageRequest.of(0, PAGE_SIZE));
                    if (page.isEmpty()) {
                        break;
                    }
                    process(page, mode, snapshot, checkOwnership, progress);
                    afterId = page.getLast();
                }
                log.info("장르 조합별 기본 추천 계산: {}회", snapshot.calculatedGroupCount());
            } else {
                process(targets, mode, null, checkOwnership, progress);
            }
            Result result = new Result(true, progress.processed, progress.succeeded,
                    List.copyOf(progress.failedIds), Duration.ofNanos(System.nanoTime() - startedAt));
            log.info("추천 계산 완료: 처리={}, 성공={}, 실패={}, 소요시간={}초, 실패 ID={}",
                    result.processed(), result.succeeded(), result.failedIds().size(),
                    result.elapsed().toSeconds(), result.failedIds());
            return result;
        }).orElseGet(() -> {
            log.warn("추천 계산이 이미 실행 중입니다. 추가 작업을 시작하지 않습니다.");
            return new Result(false, 0, 0, List.of(), Duration.ofNanos(System.nanoTime() - startedAt));
        });
    }

    private void process(List<Long> ids, RelatedGamePrecomputeMode mode, RelatedGamePrecomputeSnapshot snapshot,
                         Runnable checkOwnership, Progress progress) {
        for (Long gameId : ids) {
            checkOwnership.run();
            try {
                if (snapshot == null) {
                    precomputeService.precompute(gameId, mode);
                } else {
                    precomputeService.precompute(gameId, mode, snapshot);
                }
                progress.succeeded++;
            } catch (RuntimeException failure) {
                progress.failedIds.add(gameId);
                log.error("추천 계산 실패: gameId={}, error={}", gameId, failure.toString());
            }
            progress.processed++;
            // 연결 단절은 개별 게임 실패로 삼키지 않고 전체 실행을 중단한다.
            checkOwnership.run();
            if (progress.processed % PAGE_SIZE == 0) {
                log.info("추천 계산 진행: 처리={}, 성공={}, 실패={}",
                        progress.processed, progress.succeeded, progress.failedIds.size());
            }
        }
    }

    private static class Progress {
        private long processed;
        private long succeeded;
        private final List<Long> failedIds = new ArrayList<>();
    }

    public record Result(boolean acquired, long processed, long succeeded,
                         List<Long> failedIds, Duration elapsed) {
        public int exitCode() {
            return !acquired ? 2 : failedIds.isEmpty() ? 0 : 1;
        }
    }
}
