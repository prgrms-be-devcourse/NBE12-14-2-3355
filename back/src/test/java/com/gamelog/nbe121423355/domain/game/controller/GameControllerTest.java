package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @Test
    @DisplayName("파라미터를 생략하면 첫 페이지를 20개씩 조회한다")
    void pageUsesDefaults() throws Exception {
        // given
        assertThat(gameRepository.count()).isZero();

        gameRepository.save(createGame(1001L, "첫 번째 게임"));
        gameRepository.save(createGame(1002L, "두 번째 게임"));

        // when & then
        mockMvc.perform(get("/api/v1/games/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("첫 번째 게임"))
                .andExpect(jsonPath("$.data.content[1].title")
                        .value("두 번째 게임"))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("요청한 페이지의 게임 목록과 페이지 정보를 반환한다")
    void pageReturnsRequestedPage() throws Exception {
        // given
        assertThat(gameRepository.count()).isZero();

        gameRepository.save(createGame(1001L, "첫 번째 게임"));
        gameRepository.save(createGame(1002L, "두 번째 게임"));
        gameRepository.save(createGame(1003L, "세 번째 게임"));

        // when & then
        mockMvc.perform(get("/api/v1/games/page")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("세 번째 게임"))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("저장된 게임이 없으면 빈 페이지를 반환한다")
    void pageReturnsEmptyPage() throws Exception {
        // given
        assertThat(gameRepository.count()).isZero();

        // when & then
        mockMvc.perform(get("/api/v1/games/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @ParameterizedTest
    @CsvSource({
            "page, -1",
            "size, 0",
            "size, 101",
            "page, abc",
            "size, abc",
            "sort, unknown"
    })
    @DisplayName("잘못된 페이지 파라미터는 400 응답을 반환한다")
    void pageRejectsInvalidParameters(
            String parameter,
            String value
    ) throws Exception {
        mockMvc.perform(get("/api/v1/games/page")
                        .param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("keyword 쿼리 파라미터로 제목을 검색하고 결과만 페이징한다")
    void pageSearchesByKeyword() throws Exception {
        assertThat(gameRepository.count()).isZero();
        gameRepository.save(createGame(1001L, "Super Mario"));
        gameRepository.save(createGame(1002L, "The Legend of Zelda"));
        gameRepository.save(createGame(1003L, "ZELDA Adventure"));

        mockMvc.perform(get("/api/v1/games/page")
                        .param("keyword", " zelda ")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("ZELDA Adventure"))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    private Game createGame(Long igdbId, String title) {
        return createGame(igdbId, title, 1704067200L);
    }

    @Test
    @DisplayName("제목순 정렬은 검색 결과에 적용되며 동일 제목은 ID순으로 페이징한다")
    void sortsByTitle() throws Exception {
        gameRepository.save(createGame(8001L, "Zelda Z"));
        Game first = gameRepository.save(createGame(8002L, "Zelda A"));
        Game second = gameRepository.save(createGame(8003L, "Zelda A"));
        gameRepository.save(createGame(8004L, "Mario"));

        mockMvc.perform(get("/api/v1/games/page")
                        .param("keyword", "zelda").param("sort", "TITLE")
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(first.getId().intValue()))
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(get("/api/v1/games/page")
                        .param("keyword", "zelda").param("sort", "TITLE")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(second.getId().intValue()));
    }

    @Test
    @DisplayName("최신 출시일순으로 조회하고 같은 출시일은 ID순으로 정렬한다")
    void sortsByLatestRelease() throws Exception {
        gameRepository.save(createGame(8101L, "Old", 1577836800L));
        gameRepository.save(createGame(8102L, "New B", 1704067200L));
        gameRepository.save(createGame(8103L, "New A", 1704067200L));
        gameRepository.save(createGame(8104L, "Unknown", null));

        mockMvc.perform(get("/api/v1/games/page").param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("New B"))
                .andExpect(jsonPath("$.data.content[1].title").value("New A"))
                .andExpect(jsonPath("$.data.content[2].title").value("Old"))
                .andExpect(jsonPath("$.data.content[3].title").value("Unknown"));
    }

    private Game createGame(Long igdbId, String title, Long releaseTimestamp) {
        IgdbGameResponse response = new IgdbGameResponse(
                igdbId,
                title,
                "테스트 게임 설명",
                new IgdbGameResponse.Cover(
                        1L,
                        "//images.igdb.com/test.jpg"
                ),
                releaseTimestamp,
                new BigDecimal("90.50"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        return Game.createFromIgdb(response);
    }
}
