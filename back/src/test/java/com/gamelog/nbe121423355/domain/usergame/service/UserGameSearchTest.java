package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.*;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserGameSearchTest {
    @Autowired EntityManager em;
    @Autowired UserGameService service;
    @Autowired MockMvc mvc;
    User owner;
    Platform pc;
    Platform console;
    Genre rpg;
    Genre action;
    long nextIgdbId = 82000L;

    @BeforeEach
    void setup() {
        owner = new User("library", "library@test.com", "password");
        em.persist(owner);
        pc = Platform.createFromIgdb(81001L, "Library PC");
        console = Platform.createFromIgdb(81002L, "Library Console");
        rpg = Genre.createFromIgdb(81001L, "Library RPG");
        action = Genre.createFromIgdb(81002L, "Library Action");
        List.of(pc, console, rpg, action).forEach(em::persist);
        save(owner, "가 게임", PlayStatus.COMPLETED, false, false, false, pc, 10, 1, "4.5", List.of(rpg, action));
        save(owner, "나 게임", null, true, true, false, console, 20, 2, "2.0", List.of(action));
        save(owner, "다 100%_!", null, false, false, true, null, null, null, null, List.of());
        User other = new User("other", "other@test.com", "password");
        em.persist(other);
        save(other, "Other", PlayStatus.PLAYED, true, true, true, pc, 100, 3, "5.0", List.of(rpg));
        UserGame removed = save(owner, "Removed", null, false, false, false, null, null, null, null, List.of());
        removed.updatePlayRecord(null, false, false, false, true, null, null, null, null, null, null, null);
        em.flush();
        em.clear();
    }

    @Test
    void defaultsOnlyReturnOwnersLibraryIncludingGamesWithoutPlatform() {
        assertThat(search(new UserGameSearchRequest()).getContent()).extracting(UserGameListResponse::title)
                .containsExactly("나 게임", "가 게임", "다 100%_!");
    }

    @Test
    void filtersEachStatusIndependently() {
        for (UserGameTab tab : List.of(UserGameTab.PLAYED, UserGameTab.PLAYING, UserGameTab.BACKLOG, UserGameTab.WISHLIST, UserGameTab.LIKED)) {
            UserGameSearchRequest request = new UserGameSearchRequest();
            request.setStatus(tab);
            String expected = switch (tab) {
                case PLAYED -> "가 게임";
                case PLAYING, BACKLOG -> "나 게임";
                case WISHLIST -> "다 100%_!";
                case LIKED -> "Removed";
                default -> throw new IllegalArgumentException("지원하지 않는 상태: " + tab);
            };
            assertThat(search(request).getContent()).extracting(UserGameListResponse::title).containsExactly(expected);
        }
    }

    @Test
    void combinesFiltersAndPaginatesWithoutGenreDuplicates() {
        UserGameSearchRequest request = new UserGameSearchRequest();
        request.setPlatformIds(List.of(pc.getId(), console.getId()));
        request.setGenreIds(List.of(rpg.getId(), action.getId()));
        request.setKeyword(" 게임 ");
        request.setSize(1);
        Page<UserGameListResponse> first = search(request);
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(UserGameListResponse::title).containsExactly("나 게임");
        request.setPage(1);
        assertThat(search(request).getContent()).extracting(UserGameListResponse::title).containsExactly("가 게임");
        request.setPage(0);
        request.setPlatformIds(List.of(console.getId()));
        request.setGenreIds(List.of(rpg.getId()));
        assertThat(search(request)).isEmpty();
    }

    @Test
    void usesRecordedPlatformInsteadOfSupportedPlatforms() {
        UserGameSearchRequest request = new UserGameSearchRequest();
        request.setPlatformIds(List.of(pc.getId()));
        assertThat(search(request).getContent()).extracting(UserGameListResponse::title).containsExactly("가 게임");
    }

    @Test
    void treatsWildcardsAsLiteralTextAndEmptyFiltersAsAll() {
        UserGameSearchRequest request = new UserGameSearchRequest();
        request.setKeyword(" %_! ");
        request.setGenreIds(List.of());
        request.setPlatformIds(List.of());
        assertThat(search(request).getContent()).extracting(UserGameListResponse::title).containsExactly("다 100%_!");
        request.setKeyword("  ");
        assertThat(search(request).getTotalElements()).isEqualTo(3);
    }

    @Test
    void sortsByPersonalRatingTitleAndPlayTimeWithNullsLast() {
        UserGameSearchRequest request = new UserGameSearchRequest();
        for (UserGameSort sort : List.of(UserGameSort.RATING, UserGameSort.TITLE)) {
            request.setSort(sort);
            assertThat(search(request).getContent()).extracting(UserGameListResponse::title)
                    .containsExactly("가 게임", "나 게임", "다 100%_!");
        }
        request.setSort(UserGameSort.PLAY_TIME);
        assertThat(search(request).getContent()).extracting(UserGameListResponse::title)
                .containsExactly("나 게임", "가 게임", "다 100%_!");
    }

    @Test
    void endpointBindsFiltersAndPreservesResponseShape() throws Exception {
        mvc.perform(get("/api/v1/library/games")
                        .with(user(new SecurityUser(owner.getId(), List.of())))
                        .param("status", "PLAYED").param("sort", "RATING")
                        .param("genreIds", rpg.getId() + "," + action.getId())
                        .param("platformIds", pc.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.userGames[0].title").value("가 게임"));
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        for (String[] param : List.of(new String[]{"status", "INVALID"}, new String[]{"sort", "INVALID"},
                new String[]{"page", "-1"}, new String[]{"size", "0"}, new String[]{"size", "101"},
                new String[]{"platformIds", "-1"}, new String[]{"genreIds", "text"})) {
            mvc.perform(get("/api/v1/library/games").with(user(new SecurityUser(owner.getId(), List.of())))
                            .param(param[0], param[1])).andExpect(status().isBadRequest());
        }
    }

    private Page<UserGameListResponse> search(UserGameSearchRequest request) {
        return service.getUserGameList(owner.getId(), request);
    }

    private UserGame save(User user, String title, PlayStatus status, boolean playing, boolean backlog,
                          boolean wishlist, Platform platform, Integer hours, Integer day, String rating, List<Genre> genres) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(nextIgdbId++, title, null, null, null, null,
                List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        genres.forEach(genre -> em.persist(new GameGenre(game, genre)));
        // Both games support PC, but only one user record uses PC.
        em.persist(new GamePlatform(game, pc));
        UserGame record = new UserGame(user, game, status, playing, backlog, wishlist, false, platform,
                hours == null ? null : BigDecimal.valueOf(hours), null, null, null, null,
                day == null ? null : LocalDateTime.of(2026, 9, day, 12, 0));
        em.persist(record);
        if (rating != null) em.persist(new Review(record, new BigDecimal(rating), null, false));
        return record;
    }
}
