package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;
import com.gamelog.nbe121423355.domain.game.dto.GameSort;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GameCatalogPolicyTest extends GameQueryTestSupport {
    @Test
    @DisplayName("기본순은 IGDB 평점 내림차순이며 동점은 ID순, 평점 미등록은 마지막이다")
    void defaultSortUsesIgdbRatingBeforePagination() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        save(9101L, "Rated Missing", date, null);
        save(9102L, "Rated Low", date, "70");
        save(9103L, "Rated High First", date, "95");
        save(9104L, "Rated High Second", date, "95");
        save(9105L, "Rated Zero", date, "0");
        entityManager.flush();

        List<String> expected = List.of("Rated High First", "Rated High Second", "Rated Low", "Rated Zero", "Rated Missing");
        for (int page = 0; page < expected.size(); page++) {
            var result = gameService.getGamesPage(new GameSearchRequest(page, 1, null, "Rated", null, null));
            assertThat(result.getContent()).extracting(GameListResponse::title).containsExactly(expected.get(page));
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("모든 정렬과 자동완성은 1970년 1월 1일부터 오늘까지 출시된 게임만 반환한다")
    void filtersReleaseDateBoundariesForEverySortAndSuggestions() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        save(9201L, "Boundary Old", LocalDate.of(1969, 12, 31), "100");
        save(9202L, "Boundary Unknown", null, "100");
        save(9203L, "Boundary Future", today.plusDays(1), "100");
        save(9204L, "Boundary Earliest", LocalDate.of(1970, 1, 1), "80");
        save(9205L, "Boundary Current", today, "90");
        entityManager.flush();

        assertThat(gameService.getSuggestions("Boundary"))
                .extracting(GameListResponse::title)
                .containsExactlyInAnyOrder("Boundary Earliest", "Boundary Current");

        Stream.concat(Stream.<GameSort>of((GameSort) null), Arrays.stream(GameSort.values())).forEach(sort -> {
            var first = gameService.getGamesPage(new GameSearchRequest(0, 1, sort, "Boundary", null, null));
            var second = gameService.getGamesPage(new GameSearchRequest(1, 1, sort, "Boundary", null, null));
            assertThat(first.getTotalElements()).isEqualTo(2);
            assertThat(first.getTotalPages()).isEqualTo(2);
            assertThat(second.getTotalElements()).isEqualTo(2);
            var beyond = gameService.getGamesPage(new GameSearchRequest(2, 1, sort, "Boundary", null, null));
            assertThat(beyond.getContent()).isEmpty();
            assertThat(beyond.getTotalElements()).isEqualTo(2);
            assertThat(Stream.concat(first.getContent().stream(), second.getContent().stream())
                    .map(GameListResponse::title).toList())
                    .containsExactlyInAnyOrder("Boundary Earliest", "Boundary Current");
        });
    }

    private void save(Long id, String title, LocalDate date, String rating) {
        entityManager.persist(Game.createFromIgdb(new IgdbGameResponse(
                id, title, null, null,
                date == null ? null : date.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
                rating == null ? null : new BigDecimal(rating),
                List.of(), List.of(), List.of(), List.of())));
    }
}
