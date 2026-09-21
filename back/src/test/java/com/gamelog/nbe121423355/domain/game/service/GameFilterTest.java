package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameFilterTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    private Genre rpg;
    private Genre action;
    private Platform pc;
    private Platform console;

    @BeforeEach
    void setUp() {
        rpg = Genre.createFromIgdb(9001L, "Filter RPG");
        action = Genre.createFromIgdb(9002L, "Filter Action");
        pc = Platform.createFromIgdb(9001L, "Filter PC");
        console = Platform.createFromIgdb(9002L, "Filter Console");
        entityManager.persist(rpg);
        entityManager.persist(action);
        entityManager.persist(pc);
        entityManager.persist(console);

        saveGame(9001L, "Zelda Both", List.of(rpg, action), List.of(pc, console));
        saveGame(9002L, "Zelda RPG", List.of(rpg), List.of(console));
        saveGame(9003L, "Mario Action", List.of(action), List.of(pc));
        saveGame(9004L, "Unclassified", List.of(), List.of());
        entityManager.flush();
        entityManager.clear();
    }

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

    @Test
    @DisplayName("검색 후보는 제한 전에 완전 일치, 시작 일치, 포함 일치 순으로 정렬한다")
    void ranksSuggestionsBeforeLimiting() throws Exception {
        for (int i = 0; i < 8; i++) {
            saveGame(9100L + i, "A" + i + " Thief", List.of(), List.of());
        }
        saveGame(9200L, "Thief II: The Metal Age", List.of(), List.of());
        saveGame(9201L, "Thief", List.of(), List.of());
        mockMvc.perform(get("/api/v1/games/suggestions").param("keyword", " tHiEf "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].title").value("Thief"))
                .andExpect(jsonPath("$.data[1].title").value("Thief II: The Metal Age"))
                .andExpect(jsonPath("$.data[2].title").value("A0 Thief"));
    }

    @Test
    @DisplayName("검색 후보는 LIKE 특수문자를 문자 그대로 검색하며 빈 검색에는 빈 목록을 반환한다")
    void suggestionsEscapeWildcardsAndHandleEmptyInput() throws Exception {
        saveGame(9300L, "100%_!", List.of(), List.of());
        saveGame(9301L, "100abc", List.of(), List.of());
        assertThat(gameService.getSuggestions("%_!"))
                .extracting(GameListResponse::title).containsExactly("100%_!");
        assertThat(gameService.getSuggestions("no-such-title-1234")).isEmpty();
        assertThat(gameService.getSuggestions("   ")).isEmpty();
        mockMvc.perform(get("/api/v1/games/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("사용자 평균과 라이브러리 등록 수를 집계한 뒤 페이징한다")
    void sortsByCommunityStatistics() throws Exception {
        record("Zelda Both", "one", "5.0", "2", true);
        record("Zelda Both", "two", "1.0", "4", true);
        record("Zelda RPG", "three", "4.0", "20", true);
        record("Zelda RPG", "four", null, null, false);
        record("Zelda RPG", "five", null, null, false);
        // 평균은 Both=3, RPG=4. 라이브러리는 Both=2, RPG=1.
        for (String sort : List.of("RATING", "PLAY_TIME", "LIBRARY")) {
            String first = sort.equals("LIBRARY") ? "Zelda Both" : "Zelda RPG";
            String second = sort.equals("LIBRARY") ? "Zelda RPG" : "Zelda Both";
            mockMvc.perform(get("/api/v1/games/page").param("sort", sort)
                            .param("keyword", "zelda").param("genreIds", rpg.getId().toString())
                            .param("platformIds", console.getId().toString()).param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value(first))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
            mockMvc.perform(get("/api/v1/games/page").param("sort", sort)
                            .param("keyword", "zelda").param("size", "1").param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value(second));
        }
    }

    @Test
    @DisplayName("사용자 기록이 없으면 모든 집계 정렬은 ID순으로 게임을 반환한다")
    void communitySortWithoutRecords() throws Exception {
        for (String sort : List.of("RATING", "LIBRARY", "PLAY_TIME")) {
            mockMvc.perform(get("/api/v1/games/page").param("sort", sort))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(4))
                    .andExpect(jsonPath("$.data.content[0].title").value("Zelda Both"))
                    .andExpect(jsonPath("$.data.content[3].title").value("Unclassified"));
        }
    }

    @Test
    @DisplayName("평점 정렬은 삭제와 숨김 리뷰를 제외하고 복원하면 다시 반영한다")
    void ratingSortExcludesInactiveReviews() throws Exception {
        record("Zelda Both", "active", "5.0", null, true);
        Review deleted = record("Zelda Both", "deleted", "1.0", null, true);
        Review hidden = record("Zelda Both", "hidden", "1.0", null, true);
        record("Zelda RPG", "other", "4.0", null, true);
        deleted.deleteByUser();
        hidden.hideByAdmin();
        assertFirstRatedGame("Zelda Both");
        deleted.restore(new BigDecimal("1.0"), "restored", false);
        assertFirstRatedGame("Zelda RPG");
    }

    @Test
    @DisplayName("상태가 null인 기존 리뷰는 포함하고 모든 평점이 삭제되면 0으로 정렬한다")
    void ratingSortSupportsLegacyAndDeletedOnlyReviews() throws Exception {
        Review legacy = record("Zelda RPG", "legacy", "5.0", null, true);
        record("Zelda Both", "active", "4.0", null, true);
        entityManager.createQuery("update Review r set r.status = null where r.id = :id")
                .setParameter("id", legacy.getId()).executeUpdate();
        entityManager.refresh(legacy);
        assertFirstRatedGame("Zelda RPG");
        legacy.deleteByUser();
        assertFirstRatedGame("Zelda Both");
        mockMvc.perform(get("/api/v1/games/page").param("sort", "RATING").param("keyword", "zelda"))
                .andExpect(jsonPath("$.data.content[1].title").value("Zelda RPG"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    private void assertFirstRatedGame(String title) throws Exception {
        entityManager.flush();
        mockMvc.perform(get("/api/v1/games/page").param("sort", "RATING")
                        .param("keyword", "zelda").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value(title))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    private Review record(String title, String name, String rating, String hours, boolean library) {
        Game game = entityManager.createQuery("select g from Game g where g.title = :title", Game.class)
                .setParameter("title", title).getSingleResult();
        User user = new User(name, name + "@sort.test", "test-password");
        entityManager.persist(user);
        UserGame userGame = new UserGame(user, game);
        userGame.updatePlayRecord(null, library, false, false, true, null,
                hours == null ? null : new BigDecimal(hours), null, null, null, null, null);
        entityManager.persist(userGame);
        Review review = new Review(userGame, rating == null ? null : new BigDecimal(rating), "review", false);
        entityManager.persist(review);
        entityManager.flush();
        return review;
    }

    private Page<GameListResponse> search(String keyword, List<Long> genres,
                                          List<Long> platforms, int page, int size) {
        return gameService.getGamesPage(keyword, genres, platforms,
                PageRequest.of(page, size, Sort.by("id")));
    }

    private void saveGame(Long igdbId, String title, List<Genre> genres, List<Platform> platforms) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(
                igdbId, title, null, null, null, null,
                List.of(), List.of(), List.of(), List.of()));
        entityManager.persist(game);
        genres.forEach(genre -> entityManager.persist(new GameGenre(game, genre)));
        platforms.forEach(platform -> entityManager.persist(new GamePlatform(game, platform)));
    }
}
