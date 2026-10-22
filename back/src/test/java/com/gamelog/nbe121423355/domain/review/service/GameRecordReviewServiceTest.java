package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRecordReviewServiceTest {

    private static final long USER_ID = 1L;
    private static final long GAME_ID = 6L;
    private static final long USER_GAME_ID = 100L;

    @Mock UserGameService userGameService;
    @Mock ReviewService reviewService;
    @Mock UserGame userGame;
    @Mock User user;
    @Mock Game game;

    @InjectMocks GameRecordReviewService service;

    private UserGameReqBody userGameRequest;

    @BeforeEach
    void setUp() {
        userGameRequest = new UserGameReqBody(
                null, false, true, false, false,
                null, null, null, null, null, null, null
        );
        lenient().when(userGame.getId()).thenReturn(USER_GAME_ID);
        lenient().when(userGame.getUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(userGame.getGame()).thenReturn(game);
        lenient().when(game.getId()).thenReturn(GAME_ID);
        lenient().when(userGame.isBacklog()).thenReturn(true);
        lenient().when(userGameService.prepareGameRecordForReview(USER_ID, GAME_ID, userGameRequest))
                .thenReturn(new UserGameSaveResult(userGame, true));
    }

    @Test
    @DisplayName("리뷰가 없으면 게임 기록만 저장하고 기존 리뷰를 조회한다")
    void saveGameRecordWithoutReview() {
        ReviewResponse existing = org.mockito.Mockito.mock(ReviewResponse.class);
        when(reviewService.getReviewByUserGameId(USER_GAME_ID)).thenReturn(existing);

        DetailedReviewResponse response = service.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(userGameRequest, null)
        );

        assertThat(response.userGame().backlog()).isTrue();
        assertThat(response.review()).isSameAs(existing);
        verify(userGameService).synchronizeLibraryState(userGame);
        verify(reviewService, never()).saveReview(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("빈 리뷰 객체는 기존 리뷰를 덮어쓰지 않는다")
    void preserveExistingReviewForEmptyRequest() {
        ReviewResponse existing = org.mockito.Mockito.mock(ReviewResponse.class);
        when(reviewService.getReviewByUserGameId(USER_GAME_ID)).thenReturn(existing);

        DetailedReviewResponse response = service.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(
                        userGameRequest,
                        new ReviewSaveRequest(null, "   ", false)
                )
        );

        assertThat(response.review()).isSameAs(existing);
        verify(reviewService, never()).saveReview(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("리뷰가 있으면 저장된 게임 기록 ID로 리뷰 저장을 위임한다")
    void saveGameRecordAndReview() {
        ReviewSaveRequest reviewRequest = new ReviewSaveRequest(
                new BigDecimal("4.5"),
                "리뷰",
                false
        );
        ReviewResponse savedReview = org.mockito.Mockito.mock(ReviewResponse.class);
        when(reviewService.saveReview(USER_ID, USER_GAME_ID, reviewRequest)).thenReturn(savedReview);

        DetailedReviewResponse response = service.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(userGameRequest, reviewRequest)
        );

        assertThat(response.review()).isSameAs(savedReview);
        verify(reviewService).saveReview(USER_ID, USER_GAME_ID, reviewRequest);
        verify(reviewService, never()).getReviewByUserGameId(USER_GAME_ID);
    }

    @Test
    @DisplayName("리뷰 저장 실패를 삼키지 않고 상위 트랜잭션으로 전파한다")
    void propagateReviewFailure() {
        ReviewSaveRequest reviewRequest = new ReviewSaveRequest(
                new BigDecimal("4.5"),
                "리뷰",
                false
        );
        RuntimeException failure = new RuntimeException("리뷰 저장 실패");
        when(reviewService.saveReview(USER_ID, USER_GAME_ID, reviewRequest)).thenThrow(failure);

        assertThatThrownBy(() -> service.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(userGameRequest, reviewRequest)
        )).isSameAs(failure);
    }

    @Test
    @DisplayName("리뷰 단독 저장 후 라이브러리 상태를 동기화한다")
    void saveReviewAndSynchronizeLibrary() {
        ReviewSaveRequest request = new ReviewSaveRequest(new BigDecimal("4.0"), null, false);
        ReviewResponse savedReview = org.mockito.Mockito.mock(ReviewResponse.class);
        when(savedReview.userGameId()).thenReturn(USER_GAME_ID);
        when(reviewService.saveReview(USER_ID, USER_GAME_ID, request)).thenReturn(savedReview);

        ReviewResponse response = service.saveReview(USER_ID, USER_GAME_ID, request);

        assertThat(response).isSameAs(savedReview);
        verify(userGameService).synchronizeLibraryState(USER_GAME_ID);
    }

    @Test
    @DisplayName("리뷰 수정 후 라이브러리 상태를 동기화한다")
    void updateReviewAndSynchronizeLibrary() {
        long reviewId = 10L;
        ReviewSaveRequest request = new ReviewSaveRequest(null, "본문만 남김", false);
        ReviewResponse updatedReview = org.mockito.Mockito.mock(ReviewResponse.class);
        when(updatedReview.userGameId()).thenReturn(USER_GAME_ID);
        when(reviewService.updateReview(USER_ID, reviewId, request)).thenReturn(updatedReview);

        ReviewResponse response = service.updateReview(USER_ID, reviewId, request);

        assertThat(response).isSameAs(updatedReview);
        verify(userGameService).synchronizeLibraryState(USER_GAME_ID);
    }

    @Test
    @DisplayName("리뷰 삭제 후 라이브러리 상태를 동기화한다")
    void deleteReviewAndSynchronizeLibrary() {
        long reviewId = 10L;
        long version = 3L;
        when(reviewService.deleteReview(USER_ID, reviewId, version)).thenReturn(USER_GAME_ID);

        service.deleteReview(USER_ID, reviewId, version);

        verify(userGameService).synchronizeLibraryState(USER_GAME_ID);
    }
}
