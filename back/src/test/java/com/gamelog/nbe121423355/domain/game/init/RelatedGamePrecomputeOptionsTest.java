package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelatedGamePrecomputeOptionsTest {

    @Test
    void parsesAllAndRequiresExplicitMode() {
        var options = parse("--all", "--mode=INITIAL_LOAD");
        assertThat(options.all()).isTrue();
        assertThat(options.gameIds()).isEmpty();
        assertThat(options.mode()).isEqualTo(RelatedGamePrecomputeMode.INITIAL_LOAD);
    }

    @Test
    void normalizesSpecifiedIds() {
        var options = parse("--game-ids=30, 10,30,20", "--mode=REFRESH");
        assertThat(options.all()).isFalse();
        assertThat(options.gameIds()).containsExactly(10L, 20L, 30L);
    }

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void rejectsInvalidTargetsBeforeDatabaseAccess(String[] args) {
        assertThatThrownBy(() -> parse(args)).isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidArguments() {
        return Stream.<String[]>of(
                new String[]{"--mode=REFRESH"},
                new String[]{"--all", "--game-ids=1", "--mode=REFRESH"},
                new String[]{"--all=false", "--mode=REFRESH"},
                new String[]{"--all"},
                new String[]{"--all", "--mode=invalid"},
                new String[]{"--all", "--mode=REFRESH", "--mode=INITIAL_LOAD"},
                new String[]{"--game-ids=", "--mode=REFRESH"},
                new String[]{"--game-ids=1,", "--mode=REFRESH"},
                new String[]{"--game-ids=-1", "--mode=REFRESH"},
                new String[]{"--game-ids=0", "--mode=REFRESH"},
                new String[]{"--game-ids=abc", "--mode=REFRESH"},
                new String[]{"--all", "--mode=REFRESH", "--unknown=true"}
        ).map(args -> Arguments.of((Object) args));
    }

    private RelatedGamePrecomputeOptions parse(String... args) {
        return RelatedGamePrecomputeOptions.parse(new DefaultApplicationArguments(args));
    }
}
