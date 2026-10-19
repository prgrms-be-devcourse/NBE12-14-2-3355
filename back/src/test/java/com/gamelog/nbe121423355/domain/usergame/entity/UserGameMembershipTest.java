package com.gamelog.nbe121423355.domain.usergame.entity;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserGameMembershipTest {
    private final User user = new User("test", "test@example.com", "password");
    private final Game game = mock(Game.class);

    @Test
    @DisplayName("좋아요만 있는 게임은 생성·수정·상태 변경 시 라이브러리에 포함되지 않는다")
    void likedOnlyIsExcludedAcrossCreationAndUpdate() {
        UserGame record = new UserGame(user, game, null, false, false, false, true,
                null, null, null, null, null, null, null);
        assertThat(record.isInLibrary()).isFalse();
        assertThat(record.isLiked()).isTrue();
        record.updatePlayRecord(null, false, false, false, true,
                null, null, null, null, null, null, null);
        assertThat(record.isInLibrary()).isFalse();
        UserGame quick = new UserGame(user, game);
        quick.changeLiked(true);
        assertThat(quick.isInLibrary()).isFalse();
    }

    static Stream<Consumer<UserGame>> stateCycles() {
        return Stream.of(
            r -> { r.changePlayStatus(PlayStatus.PLAYED); assertThat(r.isInLibrary()).isTrue(); r.clearPlayStatus(); },
            r -> { r.changePlaying(true); assertThat(r.isInLibrary()).isTrue(); r.changePlaying(false); },
            r -> { r.changeWishlist(true); assertThat(r.isInLibrary()).isTrue(); r.changeWishlist(false); },
            r -> { r.changeBacklog(true); assertThat(r.isInLibrary()).isTrue(); r.changeBacklog(false); }
        );
    }

    @ParameterizedTest
    @MethodSource("stateCycles")
    @DisplayName("마지막 상태를 해제하면 라이브러리에서 제외되고 좋아요는 유지된다")
    void finalStateRemovalExcludesButKeepsLike(Consumer<UserGame> cycle) {
        UserGame record = new UserGame(user, game);
        record.changeLiked(true);
        cycle.accept(record);
        assertThat(record.isInLibrary()).isFalse();
        assertThat(record.isLiked()).isTrue();
        record.changeWishlist(true);
        assertThat(record.isInLibrary()).isTrue();
    }

    static Stream<Integer> recordFields() { return Stream.of(0, 1, 2, 3, 4, 5, 6); }

    @ParameterizedTest
    @MethodSource("recordFields")
    @DisplayName("플랫폼·양수 시간·날짜 기록이 남아 있으면 라이브러리에 유지된다")
    void anyRemainingRecordKeepsMembership(int field) {
        UserGame record = new UserGame(user, game, null, false, false, false, false,
                field == 0 ? mock(Platform.class) : null,
                field == 1 ? new BigDecimal("0.1") : null,
                field == 2 ? BigDecimal.ONE : null,
                field == 3 ? BigDecimal.ONE : null,
                field == 4 ? LocalDate.now() : null,
                field == 5 ? LocalDate.now() : null,
                field == 6 ? LocalDateTime.now() : null);
        assertThat(record.isInLibrary()).isTrue();
        record.changePlaying(true);
        record.changePlaying(false);
        assertThat(record.isInLibrary()).isTrue();
        record.updatePlayRecord(null, false, false, false, false,
                null, null, null, null, null, null, null);
        assertThat(record.isInLibrary()).isFalse();
    }
    static Stream<BigDecimal> zeroHours() {
        return Stream.of(BigDecimal.ZERO, new BigDecimal("0.0"), new BigDecimal("0.00"));
    }

    @ParameterizedTest
    @MethodSource("zeroHours")
    @DisplayName("0시간은 생성·수정·상태 변경 시 라이브러리 포함 조건에서 제외된다")
    void zeroHoursDoNotCountAcrossCreationAndUpdate(BigDecimal zero) {
        UserGame record = new UserGame(user, game, null, false, false, false, true,
                null, zero, zero, zero, null, null, null);
        assertThat(record.isInLibrary()).isFalse();
        record.changePlaying(true);
        assertThat(record.isInLibrary()).isTrue();
        record.changePlaying(false);
        assertThat(record.isInLibrary()).isFalse();
        record.updatePlayRecord(null, false, false, false, true,
                null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null, null);
        assertThat(record.isInLibrary()).isTrue();
        record.updatePlayRecord(null, false, false, false, true,
                null, zero, zero, zero, null, null, null);
        assertThat(record.isInLibrary()).isFalse();
        assertThat(record.isLiked()).isTrue();
    }

}
