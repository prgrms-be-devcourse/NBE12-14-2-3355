package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatedGamePrecomputeBatchServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private RelatedGamePrecomputeService precomputeService;
    @Mock
    private RelatedGamePrecomputeLock lock;

    private RelatedGamePrecomputeBatchService batch;

    @BeforeEach
    void setUp() {
        batch = new RelatedGamePrecomputeBatchService(gameRepository, precomputeService, lock);
    }

    private void allowLock() {
        when(lock.tryRun(any())).thenAnswer(invocation -> {
            Function<Runnable, Object> action = invocation.getArgument(0);
            return Optional.of(action.apply(() -> {}));
        });
    }

    @Test
    void skipsAllWorkWhenAnotherExecutionOwnsLock() {
        when(lock.tryRun(any())).thenReturn(Optional.empty());

        var result = batch.run(true, List.of(), RelatedGamePrecomputeMode.INITIAL_LOAD);

        assertThat(result.acquired()).isFalse();
        assertThat(result.exitCode()).isEqualTo(2);
        verifyNoInteractions(gameRepository, precomputeService);
    }

    @Test
    void continuesAfterIndividualFailureAndReturnsRetryIds() {
        allowLock();
        when(precomputeService.precompute(anyLong(), eq(RelatedGamePrecomputeMode.REFRESH)))
                .thenAnswer(invocation -> {
                    if (Long.valueOf(20).equals(invocation.getArgument(0))) {
                        throw new IllegalStateException("저장 실패");
                    }
                    return 20;
                });

        var result = batch.run(false, List.of(30L, 10L, 20L, 10L), RelatedGamePrecomputeMode.REFRESH);

        assertThat(result.processed()).isEqualTo(3);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failedIds()).containsExactly(20L);
        assertThat(result.exitCode()).isEqualTo(1);
        var order = inOrder(precomputeService);
        order.verify(precomputeService).precompute(10L, RelatedGamePrecomputeMode.REFRESH);
        order.verify(precomputeService).precompute(20L, RelatedGamePrecomputeMode.REFRESH);
        order.verify(precomputeService).precompute(30L, RelatedGamePrecomputeMode.REFRESH);
    }

    @Test
    void allUsesKeysetPagesAndFixedUpperBoundary() {
        allowLock();
        when(gameRepository.findMaxGameIdForPrecompute()).thenReturn(105L);
        when(gameRepository.findGameIdsForPrecompute(0, 105, PageRequest.of(0, 100)))
                .thenReturn(List.of(1L, 100L));
        when(gameRepository.findGameIdsForPrecompute(100, 105, PageRequest.of(0, 100)))
                .thenReturn(List.of(105L));
        when(gameRepository.findGameIdsForPrecompute(105, 105, PageRequest.of(0, 100)))
                .thenReturn(List.of());

        var result = batch.run(true, List.of(), RelatedGamePrecomputeMode.INITIAL_LOAD);

        assertThat(result.processed()).isEqualTo(3);
        assertThat(result.exitCode()).isZero();
        verify(precomputeService).precompute(105L, RelatedGamePrecomputeMode.INITIAL_LOAD);
        verify(gameRepository, times(1)).findMaxGameIdForPrecompute();
        verifyNoMoreInteractions(gameRepository);
    }

    @Test
    void lostLockStopsExecutionInsteadOfTreatingItAsIndividualGameFailure() {
        when(lock.tryRun(any())).thenAnswer(invocation -> {
            Function<Runnable, Object> action = invocation.getArgument(0);
            AtomicInteger checks = new AtomicInteger();
            Runnable check = () -> {
                if (checks.incrementAndGet() > 1) {
                    throw new IllegalStateException("잠금 연결 단절");
                }
            };
            return Optional.of(action.apply(check));
        });

        assertThatThrownBy(() -> batch.run(false, List.of(1L, 2L), RelatedGamePrecomputeMode.REFRESH))
                .isInstanceOf(IllegalStateException.class).hasMessage("잠금 연결 단절");

        verify(precomputeService).precompute(1L, RelatedGamePrecomputeMode.REFRESH);
        verify(precomputeService, never()).precompute(2L, RelatedGamePrecomputeMode.REFRESH);
    }
}
