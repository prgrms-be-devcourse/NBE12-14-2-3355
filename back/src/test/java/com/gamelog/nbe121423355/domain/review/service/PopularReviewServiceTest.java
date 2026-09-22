package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.response.PopularReviewResponse;
import com.gamelog.nbe121423355.domain.review.repository.PopularReviewProjection;
import com.gamelog.nbe121423355.domain.review.repository.ReviewLikeRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularReviewServiceTest {

    @Mock
    private ReviewLikeRepository reviewLikeRepository;

    @InjectMocks
    private PopularReviewService popularReviewService;

    @Test
    @DisplayName("인기 리뷰 Projection을 응답 DTO로 변환한다")
    void getPopularReviewsMapsProjectionToResponse() {
        // given: 인기 리뷰 Projection 한 개를 준비
        PopularReviewProjection review =
                org.mockito.Mockito.mock(PopularReviewProjection.class);
        LocalDateTime createdDate =
                LocalDateTime.of(2026, 9, 22, 12, 0);

        when(review.getReviewId()).thenReturn(1L);
        when(review.getUserId()).thenReturn(10L);
        when(review.getNickname()).thenReturn("리뷰 작성자");
        when(review.getProfileImageUrl())
                .thenReturn("https://image.test/profile.jpg");
        when(review.getGameId()).thenReturn(20L);
        when(review.getGameTitle()).thenReturn("게임 제목");
        when(review.getGameCoverImageUrl())
                .thenReturn("https://image.test/game.jpg");
        when(review.getRating()).thenReturn(new BigDecimal("4.5"));
        when(review.getContent()).thenReturn("인기 리뷰 내용");
        when(review.getSpoiler()).thenReturn(true);
        when(review.getLikeCount()).thenReturn(15L);
        when(review.getCreatedDate()).thenReturn(createdDate);

        when(reviewLikeRepository.findPopularReviews(PageRequest.of(0, 5)))
                .thenReturn(List.of(review));

        // when: 기본 크기로 인기 리뷰를 조회
        List<PopularReviewResponse> result =
                popularReviewService.getPopularReviews(5);

        // then: Projection의 모든 카드 정보가 응답 DTO에 매핑
        assertThat(result).containsExactly(new PopularReviewResponse(
                1L,
                10L,
                "리뷰 작성자",
                "https://image.test/profile.jpg",
                20L,
                "게임 제목",
                "https://image.test/game.jpg",
                new BigDecimal("4.5"),
                "인기 리뷰 내용",
                true,
                15L,
                createdDate
        ));
    }

    @Test
    @DisplayName("인기 리뷰가 없으면 빈 목록을 반환한다")
    void getPopularReviewsReturnsEmptyList() {
        // given: 인기 리뷰 조회 결과가 비어 있도록 준비
        when(reviewLikeRepository.findPopularReviews(PageRequest.of(0, 5)))
                .thenReturn(List.of());

        // when: 인기 리뷰를 조회
        List<PopularReviewResponse> result =
                popularReviewService.getPopularReviews(5);

        // then: 빈 목록을 반환
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("조회 개수가 허용 범위를 벗어나면 예외가 발생한다")
    void getPopularReviewsRejectsInvalidSize(int size) {
        // given: 허용 범위를 벗어난 조회 개수를 준비

        // when: 잘못된 조회 개수로 인기 리뷰 조회를 요청
        // then: 400 예외가 발생하고 Repository는 호출하지 않음
        assertThatThrownBy(() -> popularReviewService.getPopularReviews(size))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("400-10"));

        verifyNoInteractions(reviewLikeRepository);
    }
}
