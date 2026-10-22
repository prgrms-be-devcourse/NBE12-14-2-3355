package com.gamelog.nbe121423355.domain.game.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatedGameCalculationTest {

    private static final LocalDateTime FIRST_ATTEMPT = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Test
    void initialFailureRemainsUnreadyAndCanBeRetried() {
        RelatedGameCalculation calculation = pendingCalculation();
        assertThat(calculation.isReady()).isFalse();

        calculation.fail(FIRST_ATTEMPT, "DB 연결 실패");
        assertThat(calculation.isReady()).isFalse();
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);

        calculation.start(FIRST_ATTEMPT.plusMinutes(1));
        calculation.succeed(FIRST_ATTEMPT.plusMinutes(2));

        assertThat(calculation.isReady()).isTrue();
        assertThat(calculation.getLastError()).isNull();
    }

    @Test
    void failedRefreshPreservesPreviousSuccessAndReadiness() {
        RelatedGameCalculation calculation = pendingCalculation();
        calculation.start(FIRST_ATTEMPT);
        LocalDateTime succeededAt = FIRST_ATTEMPT.plusMinutes(1);
        calculation.succeed(succeededAt);

        calculation.start(FIRST_ATTEMPT.plusDays(1));
        assertThat(calculation.isReady()).isTrue();
        calculation.fail(FIRST_ATTEMPT.plusDays(1), "저장 실패");

        assertThat(calculation.isReady()).isTrue();
        assertThat(calculation.getLastSuccessAt()).isEqualTo(succeededAt);
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.FAILED);
        assertThat(calculation.getLastError()).isEqualTo("저장 실패");
    }

    @Test
    void successfulCalculationDoesNotRequireAnyCandidate() {
        RelatedGameCalculation calculation = pendingCalculation();
        calculation.start(FIRST_ATTEMPT);
        calculation.succeed(FIRST_ATTEMPT.plusSeconds(1));

        assertThat(calculation.isReady()).isTrue();
        assertThat(calculation.getLastAttemptStatus()).isEqualTo(RelatedGameCalculationStatus.SUCCESS);
    }

    @Test
    void longErrorDoesNotPreventFailureRecording() {
        RelatedGameCalculation calculation = pendingCalculation();
        calculation.fail(FIRST_ATTEMPT, "x".repeat(1000));

        assertThat(calculation.getLastError()).hasSize(500);
        assertThat(calculation.isReady()).isFalse();
    }

    @Test
    void pendingCalculationCannotBeMarkedSuccessfulWithoutStarting() {
        RelatedGameCalculation calculation = pendingCalculation();

        assertThatThrownBy(() -> calculation.succeed(FIRST_ATTEMPT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calculation.isReady()).isFalse();
    }

    private RelatedGameCalculation pendingCalculation() {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(1L);
        return new RelatedGameCalculation(game);
    }
}
