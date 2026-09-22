package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.dto.response.PopularReviewResponse;
import com.gamelog.nbe121423355.domain.review.service.PopularReviewService;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PopularReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopularReviewService popularReviewService;

    @Test
    @DisplayName("비로그인 사용자가 기본 5개의 인기 리뷰를 조회한다")
    void getPopularReviewsAllowsAnonymousUserWithDefaultSize() throws Exception {
        // given: 기본 크기로 반환할 인기 리뷰 응답을 준비
        LocalDateTime createdDate =
                LocalDateTime.of(2026, 9, 22, 12, 0);
        PopularReviewResponse response = new PopularReviewResponse(
                1L,
                10L,
                "리뷰 작성자",
                "https://image.test/profile.jpg",
                20L,
                "게임 제목",
                "https://image.test/game.jpg",
                new BigDecimal("4.5"),
                "인기 리뷰 내용",
                false,
                15L,
                createdDate
        );

        when(popularReviewService.getPopularReviews(5))
                .thenReturn(List.of(response));

        // when: 인증 정보와 size 없이 인기 리뷰를 조회
        // then: 공개 조회가 성공하고 기본값 5가 Service에 전달
        mockMvc.perform(get("/api/v1/reviews/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg")
                        .value("인기 리뷰를 조회했습니다."))
                .andExpect(jsonPath("$.data[0].reviewId").value(1L))
                .andExpect(jsonPath("$.data[0].userId").value(10L))
                .andExpect(jsonPath("$.data[0].nickname")
                        .value("리뷰 작성자"))
                .andExpect(jsonPath("$.data[0].profileImageUrl")
                        .value("https://image.test/profile.jpg"))
                .andExpect(jsonPath("$.data[0].gameId").value(20L))
                .andExpect(jsonPath("$.data[0].gameTitle")
                        .value("게임 제목"))
                .andExpect(jsonPath("$.data[0].gameCoverImageUrl")
                        .value("https://image.test/game.jpg"))
                .andExpect(jsonPath("$.data[0].rating").value(4.5))
                .andExpect(jsonPath("$.data[0].content")
                        .value("인기 리뷰 내용"))
                .andExpect(jsonPath("$.data[0].spoiler").value(false))
                .andExpect(jsonPath("$.data[0].likeCount").value(15L))
                .andExpect(jsonPath("$.data[0].createdDate")
                        .value("2026-09-22T12:00:00"));

        verify(popularReviewService).getPopularReviews(5);
    }

    @Test
    @DisplayName("요청한 조회 개수를 인기 리뷰 Service에 전달한다")
    void getPopularReviewsPassesRequestedSize() throws Exception {
        // given: 요청한 크기의 인기 리뷰 조회 결과를 준비
        when(popularReviewService.getPopularReviews(10))
                .thenReturn(List.of());

        // when: size를 10으로 지정해 인기 리뷰를 조회
        // then: 빈 목록 응답과 함께 요청값 10이 Service에 전달
        mockMvc.perform(get("/api/v1/reviews/popular")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(popularReviewService).getPopularReviews(10);
    }

    @Test
    @DisplayName("허용 범위를 벗어난 조회 개수는 400 응답을 반환한다")
    void getPopularReviewsRejectsInvalidSize() throws Exception {
        // given: Service가 잘못된 조회 개수 예외를 반환하도록 준비
        when(popularReviewService.getPopularReviews(51))
                .thenThrow(new ServiceException(
                        "400-10",
                        "조회 개수는 1 이상 50 이하여야 합니다."
                ));

        // when: 최대값을 초과한 size로 인기 리뷰를 조회
        // then: 검증 오류 메시지를 포함한 400 응답을 반환
        mockMvc.perform(get("/api/v1/reviews/popular")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-10"))
                .andExpect(jsonPath("$.msg")
                        .value("조회 개수는 1 이상 50 이하여야 합니다."));
    }
}
