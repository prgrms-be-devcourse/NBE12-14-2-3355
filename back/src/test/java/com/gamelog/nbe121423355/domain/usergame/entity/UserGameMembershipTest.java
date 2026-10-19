package com.gamelog.nbe121423355.domain.usergame.entity;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.user.entity.User;
import org.junit.jupiter.api.Test;
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
    void anyRemainingRecordKeepsMembership(int field) {
        UserGame record = new UserGame(user, game, null, false, false, false, false,
                field == 0 ? mock(Platform.class) : null,
                field == 1 ? BigDecimal.ZERO : null,
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
}
