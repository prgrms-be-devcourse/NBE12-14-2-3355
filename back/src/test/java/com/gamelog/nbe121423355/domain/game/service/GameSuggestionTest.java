package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameSuggestionTest extends GameQueryTestSupport {

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

}
