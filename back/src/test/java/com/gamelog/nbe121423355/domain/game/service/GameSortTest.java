package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameSortTest extends GameQueryTestSupport {

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

}
