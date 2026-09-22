package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.PopularGameResponse;
import com.gamelog.nbe121423355.domain.game.service.PopularGameService;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PopularGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopularGameService popularGameService;

    @Test
    @DisplayName("비로그인 사용자가 기본 5개의 인기 게임을 조회한다")
    void getPopularGamesAllowsAnonymousUserWithDefaultSize() throws Exception {
        // given: 기본 크기로 반환할 인기 게임 응답을 준비
        PopularGameResponse response = new PopularGameResponse(
                1L,
                "인기 게임",
                "https://image.test/game.jpg",
                10L,
                List.of(new PopularGameResponse.GenreResponse(
                        11L,
                        "Action"
                ))
        );

        when(popularGameService.getPopularGames(5))
                .thenReturn(List.of(response));

        // when: 인증 정보와 size 없이 인기 게임을 조회
        // then: 공개 조회가 성공하고 기본값 5가 Service에 전달
        mockMvc.perform(get("/api/v1/games/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg")
                        .value("인기 게임을 조회했습니다."))
                .andExpect(jsonPath("$.data[0].gameId").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("인기 게임"))
                .andExpect(jsonPath("$.data[0].coverImageUrl")
                        .value("https://image.test/game.jpg"))
                .andExpect(jsonPath("$.data[0].likeCount").value(10L))
                .andExpect(jsonPath("$.data[0].genres[0].id").value(11L))
                .andExpect(jsonPath("$.data[0].genres[0].name")
                        .value("Action"));

        verify(popularGameService).getPopularGames(5);
    }

    @Test
    @DisplayName("요청한 조회 개수를 인기 게임 Service에 전달한다")
    void getPopularGamesPassesRequestedSize() throws Exception {
        // given: 요청한 크기의 인기 게임 조회 결과를 준비
        when(popularGameService.getPopularGames(10))
                .thenReturn(List.of());

        // when: size를 10으로 지정해 인기 게임을 조회
        // then: 빈 목록 응답과 함께 요청값 10이 Service에 전달
        mockMvc.perform(get("/api/v1/games/popular")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(popularGameService).getPopularGames(10);
    }

    @Test
    @DisplayName("허용 범위를 벗어난 조회 개수는 400 응답을 반환한다")
    void getPopularGamesRejectsInvalidSize() throws Exception {
        // given: Service가 잘못된 조회 개수 예외를 반환하도록 준비
        when(popularGameService.getPopularGames(51))
                .thenThrow(new ServiceException(
                        "400-9",
                        "조회 개수는 1 이상 50 이하여야 합니다."
                ));

        // when: 최대값을 초과한 size로 인기 게임을 조회
        // then: 검증 오류 메시지를 포함한 400 응답을 반환
        mockMvc.perform(get("/api/v1/games/popular")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-9"))
                .andExpect(jsonPath("$.msg")
                        .value("조회 개수는 1 이상 50 이하여야 합니다."));
    }
}
