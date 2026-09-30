package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.RecentReviewResponse;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameListResponse;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserGameServiceRecentTest {

    @InjectMocks
    private UserGameService userGameService;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("최근 플레이한 게임을 조회한다")
    void getRecentPlayedGames() {
        Long userId = 1L;

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        given(userGame1.getGame()).willReturn(game1);
        given(userGame2.getGame()).willReturn(game2);

        given(game1.getId()).willReturn(1L);
        given(game2.getId()).willReturn(2L);

        given(game1.getTitle()).willReturn("Game 1");
        given(game2.getTitle()).willReturn("Game 2");

        given(game1.getCoverImageUrl()).willReturn("cover1");
        given(game2.getCoverImageUrl()).willReturn("cover2");

        given(userGameRepository.findRecentPlayedGames(
                eq(userId),
                any(Pageable.class)
        )).willReturn(List.of(userGame1, userGame2));

        List<UserGameListResponse> result =
                userGameService.getRecentPlayedGames(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).gameId()).isEqualTo(1L);
        assertThat(result.get(1).gameId()).isEqualTo(2L);

        verify(userGameRepository)
                .findRecentPlayedGames(
                        eq(userId),
                        any(Pageable.class)
                );
    }

    @Test
    @DisplayName("최근 플레이 게임은 최대 5개를 요청한다")
    void getRecentPlayedGamesLimit() {
        given(userGameRepository.findRecentPlayedGames(
                eq(1L),
                any(Pageable.class)
        )).willReturn(List.of());

        userGameService.getRecentPlayedGames(1L);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(userGameRepository)
                .findRecentPlayedGames(
                        eq(1L),
                        captor.capture()
                );

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("최근 작성한 리뷰를 조회한다")
    void getRecentReviews() {
        Long userId = 1L;

        Review review1 = mock(Review.class);
        Review review2 = mock(Review.class);

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        given(review1.getId()).willReturn(1L);
        given(review2.getId()).willReturn(2L);

        given(review1.getUserGame()).willReturn(userGame1);
        given(review2.getUserGame()).willReturn(userGame2);

        given(userGame1.getGame()).willReturn(game1);
        given(userGame2.getGame()).willReturn(game2);

        given(game1.getId()).willReturn(10L);
        given(game2.getId()).willReturn(20L);

        given(game1.getTitle()).willReturn("Game 1");
        given(game2.getTitle()).willReturn("Game 2");

        given(game1.getCoverImageUrl()).willReturn("cover1");
        given(game2.getCoverImageUrl()).willReturn("cover2");

        given(reviewRepository.findRecentReviews(
                eq(userId),
                any(Pageable.class)
        )).willReturn(List.of(review1, review2));

        List<RecentReviewResponse> result =
                userGameService.getRecentReviews(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).reviewId()).isEqualTo(1L);
        assertThat(result.get(0).gameId()).isEqualTo(10L);
        assertThat(result.get(1).reviewId()).isEqualTo(2L);
        assertThat(result.get(1).gameId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("최근 리뷰는 최대 3개를 요청한다")
    void getRecentReviewsLimit() {
        given(reviewRepository.findRecentReviews(
                eq(1L),
                any(Pageable.class)
        )).willReturn(List.of());

        userGameService.getRecentReviews(1L);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(reviewRepository)
                .findRecentReviews(
                        eq(1L),
                        captor.capture()
                );

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(3);
    }
}