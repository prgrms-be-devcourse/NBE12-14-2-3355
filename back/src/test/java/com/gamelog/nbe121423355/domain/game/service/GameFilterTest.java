package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameFilterTest extends GameQueryTestSupport {

    @Test
    @DisplayName("검색어 없이 장르만 선택하면 해당 장르의 게임만 반환한다")
    void filtersByGenreOnly() {
        Page<GameListResponse> result = search(null, List.of(rpg.getId()), null, 0, 20);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both", "Zelda RPG");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색어 없이 플랫폼만 선택하면 해당 플랫폼의 게임만 반환한다")
    void filtersByPlatformOnly() {
        Page<GameListResponse> result = search(null, null, List.of(pc.getId()), 0, 20);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both", "Mario Action");
    }

    @Test
    @DisplayName("장르와 플랫폼 조건을 모두 만족하는 게임만 반환한다")
    void combinesGenreAndPlatformWithAnd() {
        Page<GameListResponse> result = search(
                null, List.of(rpg.getId()), List.of(pc.getId()), 0, 20);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both");
    }

    @Test
    @DisplayName("검색어와 두 필터를 함께 적용한다")
    void combinesKeywordAndFilters() {
        Page<GameListResponse> result = search(
                " zELDa ", List.of(action.getId()), List.of(pc.getId()), 0, 20);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both");
    }

    @Test
    @DisplayName("각 필터의 다중 선택은 OR이며 중복 없이 페이징하고 전체 개수를 센다")
    void multipleSelectionsDoNotDuplicateGames() {
        List<Long> genres = List.of(rpg.getId(), action.getId());
        List<Long> platforms = List.of(pc.getId(), console.getId());
        Page<GameListResponse> first = search(null, genres, platforms, 0, 2);
        Page<GameListResponse> second = search(null, genres, platforms, 1, 2);

        assertThat(first.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both", "Zelda RPG");
        assertThat(second.getContent()).extracting(GameListResponse::title)
                .containsExactly("Mario Action");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    @DisplayName("빈 필터 목록은 필터를 적용하지 않아 연결 정보 없는 게임도 반환한다")
    void emptyFiltersReturnAllGames() {
        Page<GameListResponse> result = search(" ", List.of(), List.of(), 0, 20);
        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda Both", "Zelda RPG", "Mario Action", "Unclassified");
    }

    @Test
    @DisplayName("조건에 맞는 게임이나 필터 ID가 없으면 빈 페이지를 반환한다")
    void unmatchedFiltersReturnEmptyPage() {
        Page<GameListResponse> noMatch = search(
                "Mario", List.of(rpg.getId()), List.of(console.getId()), 0, 20);
        assertThat(noMatch.getContent()).isEmpty();
        assertThat(noMatch.getTotalElements()).isZero();
        assertThat(noMatch.getTotalPages()).isZero();
        assertThat(search(null, List.of(Long.MAX_VALUE), null, 0, 20).getContent()).isEmpty();
        assertThat(search(null, null, List.of(Long.MAX_VALUE), 0, 20).getContent()).isEmpty();
    }

    @Test
    @DisplayName("HTTP 요청의 다중 장르와 플랫폼 파라미터가 필터 조회에 전달된다")
    void bindsMultipleFilterParameters() throws Exception {
        mockMvc.perform(get("/api/v1/games/page")
                        .param("genreIds", rpg.getId().toString(), action.getId().toString())
                        .param("platformIds", pc.getId().toString(), console.getId().toString())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Mario Action"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    private Page<GameListResponse> search(String keyword, List<Long> genres,
                                          List<Long> platforms, int page, int size) {
        return gameService.getGamesPage(new GameSearchRequest(page, size, null, keyword, genres, platforms));
    }

}
