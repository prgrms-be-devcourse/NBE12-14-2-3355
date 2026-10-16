package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;
import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameOffsetSearchTest extends GameQueryTestSupport {
    @Test
    void pagesPreserveRatingAndIdOrderAndTotalIncludingEmptyPage() {
        List<Long> high = new ArrayList<>();
        List<Long> unrated = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            var game = Game.createFromIgdb(new IgdbGameResponse(
                    80000L + i, "OffsetProbe " + i, null, null, 1704067200L,
                    i % 2 == 0 ? new BigDecimal("90.00") : null,
                    List.of(), List.of(), List.of(), List.of()));
            entityManager.persist(game);
            (i % 2 == 0 ? high : unrated).add(game.getId());
        }
        entityManager.flush();
        entityManager.clear();
        List<Long> expected = new ArrayList<>(high);
        expected.addAll(unrated);
        List<Long> actual = new ArrayList<>();
        for (int page = 0; page < 4; page++) {
            var result = gameService.getGamesPage(new GameSearchRequest(
                    page, 20, null, "offsetprobe", null, null));
            assertThat(result.getTotalElements()).isEqualTo(45);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isEqualTo(page);
            assertThat(result.getNumberOfElements()).isEqualTo(page < 2 ? 20 : page == 2 ? 5 : 0);
            actual.addAll(result.getContent().stream().map(GameListResponse::id).toList());
        }
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    void literalWildcardCharactersRemainLiteral() {
        saveGame(81000L, "Literal%_!Needle", List.of(), List.of());
        saveGame(81001L, "LiteralABCNeedle", List.of(), List.of());
        entityManager.flush();
        entityManager.clear();
        var result = gameService.getGamesPage(new GameSearchRequest(
                0, 20, null, "%_!", null, null));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Literal%_!Needle");
    }
}
