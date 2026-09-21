package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final long USER_ID = 1L;
    private static final long GAME_ID = 6L;
    private static final long USER_GAME_ID = 100L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserGameService userGameService;

    @Mock
    private UserGame userGame;

    @Mock
    private User user;

    @Mock
    private Game game;

    @InjectMocks
    private ReviewService reviewService;

    private UserGameReqBody userGameRequest;

    @BeforeEach
    void setUp() {
        userGameRequest = new UserGameReqBody(
                null,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

    }

    @DisplayName("리뷰가 null이면 게임 기록만 저장한다")
    @Test
    void saveDetailedReviewWithoutReview() {
        prepareUserGameSave();
        when(reviewRepository.findByUserGame_Id(USER_GAME_ID))
                .thenReturn(Optional.empty());

        DetailedReviewResponse response = reviewService.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(userGameRequest, null)
        );

        assertThat(response.userGame().backlog()).isTrue();
        assertThat(response.review()).isNull();
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("빈 리뷰 객체면 게임 기록만 저장하고 리뷰를 생성하지 않는다")
    @Test
    void saveDetailedReviewWithEmptyReviewObject() {
        prepareUserGameSave();
        when(reviewRepository.findByUserGame_Id(USER_GAME_ID))
                .thenReturn(Optional.empty());

        DetailedReviewResponse response = reviewService.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(
                        userGameRequest,
                        new ReviewSaveRequest(null, "   ", false)
                )
        );

        assertThat(response.review()).isNull();
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("빈 리뷰 요청으로 게임 기록만 수정하면 기존 리뷰를 유지한다")
    @Test
    void preserveExistingReviewWhenReviewIsEmpty() {
        prepareUserGameSave();

        Review existingReview = org.mockito.Mockito.mock(Review.class);
        when(existingReview.getId()).thenReturn(10L);
        when(existingReview.getUserGame()).thenReturn(userGame);
        when(existingReview.getContent()).thenReturn("기존 리뷰");
        when(reviewRepository.findByUserGame_Id(USER_GAME_ID))
                .thenReturn(Optional.of(existingReview));

        DetailedReviewResponse response = reviewService.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(
                        userGameRequest,
                        new ReviewSaveRequest(null, "", false)
                )
        );

        assertThat(response.review()).isNotNull();
        assertThat(response.review().reviewId()).isEqualTo(10L);
        assertThat(response.review().content()).isEqualTo("기존 리뷰");
        verify(existingReview, never()).edit(any(), any(), anyBoolean());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("별점 없이 내용만 있는 리뷰를 저장한다")
    @Test
    void saveContentOnlyReview() {
        prepareUserGameSave();
        when(userGameRepository.findById(USER_GAME_ID))
                .thenReturn(Optional.of(userGame));
        when(reviewRepository.findIncludingDeletedByUserGameId(USER_GAME_ID))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DetailedReviewResponse response = reviewService.saveDetailedReview(
                USER_ID,
                GAME_ID,
                new DetailedReviewSaveRequest(
                        userGameRequest,
                        new ReviewSaveRequest(null, "별점 없는 리뷰", false)
                )
        );

        assertThat(response.review()).isNotNull();
        assertThat(response.review().rating()).isNull();
        assertThat(response.review().content()).isEqualTo("별점 없는 리뷰");
        verify(reviewRepository).save(any(Review.class));
    }

    @DisplayName("직접 리뷰 저장 요청이 비어 있으면 거절한다")
    @Test
    void rejectEmptyDirectReviewRequest() {
        ReviewSaveRequest emptyReview = new ReviewSaveRequest(null, " ", false);

        assertThatThrownBy(() -> reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                emptyReview
        ))
                .isInstanceOf(ServiceException.class)
                .hasMessage("별점 또는 리뷰 내용 중 하나는 입력해야 합니다.");

        verify(userGameRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("새 리뷰를 작성한다")
    @Test
    void saveNewReview() {
        prepareOwnedUserGame();
        when(userGameRepository.findById(USER_GAME_ID)).thenReturn(Optional.of(userGame));
        when(reviewRepository.findIncludingDeletedByUserGameId(USER_GAME_ID)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                new ReviewSaveRequest(new BigDecimal("4.5"), "새 리뷰", false)
        );

        assertThat(response.userGameId()).isEqualTo(USER_GAME_ID);
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(response.content()).isEqualTo("새 리뷰");
        verify(reviewRepository).save(any(Review.class));
    }

    @DisplayName("이미 리뷰가 있으면 새로 생성하지 않고 기존 리뷰를 수정한다")
    @Test
    void saveReviewUpdatesExistingReview() {
        prepareOwnedUserGame();
        Review existingReview = prepareReview();
        when(existingReview.isActive()).thenReturn(true);
        when(userGameRepository.findById(USER_GAME_ID)).thenReturn(Optional.of(userGame));
        when(reviewRepository.findIncludingDeletedByUserGameId(USER_GAME_ID))
                .thenReturn(Optional.of(existingReview));

        reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                new ReviewSaveRequest(new BigDecimal("3.5"), "수정된 리뷰", true)
        );

        verify(existingReview).edit(new BigDecimal("3.5"), "수정된 리뷰", true);
        verify(reviewRepository).flush();
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("사용자가 삭제했던 리뷰를 다시 작성하면 기존 리뷰를 복구한다")
    @Test
    void restoreDeletedReview() {
        prepareOwnedUserGame();
        Review deletedReview = prepareReview();
        when(deletedReview.isDeletedByUser()).thenReturn(true);
        when(userGameRepository.findById(USER_GAME_ID)).thenReturn(Optional.of(userGame));
        when(reviewRepository.findIncludingDeletedByUserGameId(USER_GAME_ID))
                .thenReturn(Optional.of(deletedReview));

        reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                new ReviewSaveRequest(new BigDecimal("4.0"), "다시 작성한 리뷰", false)
        );

        verify(deletedReview).restore(new BigDecimal("4.0"), "다시 작성한 리뷰", false);
        verify(reviewRepository).flush();
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("다른 사용자의 게임 기록에는 리뷰를 작성할 수 없다")
    @Test
    void rejectReviewForOtherUsersGameRecord() {
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);
        when(userGameRepository.findById(USER_GAME_ID)).thenReturn(Optional.of(userGame));

        assertThatThrownBy(() -> reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                new ReviewSaveRequest(new BigDecimal("4.0"), null, false)
        ))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("403-1"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @DisplayName("존재하지 않는 게임 기록에 리뷰를 작성하면 404 오류가 발생한다")
    @Test
    void rejectReviewWhenUserGameDoesNotExist() {
        when(userGameRepository.findById(USER_GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.saveReview(
                USER_ID,
                USER_GAME_ID,
                new ReviewSaveRequest(new BigDecimal("4.0"), null, false)
        ))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("404-2"));
    }

    @DisplayName("리뷰를 단건 조회한다")
    @Test
    void getReview() {
        prepareOwnedUserGame();
        Review review = prepareReview();
        when(review.getId()).thenReturn(10L);
        when(review.getContent()).thenReturn("조회할 리뷰");
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));

        var response = reviewService.getReview(10L);

        assertThat(response.reviewId()).isEqualTo(10L);
        assertThat(response.content()).isEqualTo("조회할 리뷰");
    }

    @DisplayName("존재하지 않는 리뷰를 조회하면 404 오류가 발생한다")
    @Test
    void getMissingReview() {
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(10L))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("404-3"));
    }

    @DisplayName("본인의 리뷰를 수정한다")
    @Test
    void updateOwnReview() {
        prepareOwnedUserGame();
        Review review = prepareReview();
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));

        reviewService.updateReview(
                USER_ID,
                10L,
                new ReviewSaveRequest(null, "글만 남긴 리뷰", true)
        );

        verify(review).edit(null, "글만 남긴 리뷰", true);
        verify(reviewRepository).flush();
    }

    @DisplayName("다른 사용자의 리뷰는 수정할 수 없다")
    @Test
    void rejectUpdateForOtherUsersReview() {
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);
        Review review = prepareReview();
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(
                USER_ID,
                10L,
                new ReviewSaveRequest(new BigDecimal("3.0"), null, false)
        ))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("403-1"));

        verify(review, never()).edit(any(), any(), anyBoolean());
        verify(reviewRepository, never()).flush();
    }

    @DisplayName("본인의 리뷰를 삭제한다")
    @Test
    void deleteOwnReview() {
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        Review review = prepareReview();
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(USER_ID, 10L);

        verify(review).deleteByUser();
        verify(reviewRepository).flush();
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @DisplayName("리뷰 소프트 삭제는 신고 확인에 필요한 원문을 보존한다")
    @Test
    void softDeleteKeepsReviewContent() {
        Review review = new Review(
                userGame,
                new BigDecimal("4.5"),
                "신고 검토에 필요한 리뷰 원문",
                true
        );

        review.deleteByUser();

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED_BY_USER);
        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getRating()).isEqualByComparingTo("4.5");
        assertThat(review.getContent()).isEqualTo("신고 검토에 필요한 리뷰 원문");
        assertThat(review.isSpoiler()).isTrue();
    }

    @DisplayName("사용자가 삭제한 리뷰를 다시 작성하면 활성 상태로 복구한다")
    @Test
    void restoreSoftDeletedReviewEntity() {
        Review review = new Review(userGame, null, "삭제 전 리뷰", false);
        review.deleteByUser();

        review.restore(new BigDecimal("3.5"), "복구한 리뷰", true);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(review.getDeletedAt()).isNull();
        assertThat(review.getRating()).isEqualByComparingTo("3.5");
        assertThat(review.getContent()).isEqualTo("복구한 리뷰");
    }

    @DisplayName("다른 사용자의 리뷰는 삭제할 수 없다")
    @Test
    void rejectDeleteForOtherUsersReview() {
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);
        Review review = prepareReview();
        when(reviewRepository.findActiveById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, 10L))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("403-1"));

        verify(review, never()).deleteByUser();
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @DisplayName("게임별 리뷰 목록을 페이지 정보와 함께 조회한다")
    @Test
    void getGameReviews() {
        prepareOwnedUserGame();
        Review review = prepareReview();
        when(review.getId()).thenReturn(10L);
        when(review.getContent()).thenReturn("게임 리뷰");
        PageRequest pageable = PageRequest.of(1, 2);
        when(reviewRepository.findByUserGame_Game_Id(GAME_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 5));

        var response = reviewService.getGameReviews(GAME_ID, pageable);

        assertThat(response.reviews()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @DisplayName("사용자별 리뷰 목록을 페이지 정보와 함께 조회한다")
    @Test
    void getUserReviews() {
        prepareOwnedUserGame();
        Review review = prepareReview();
        when(review.getId()).thenReturn(10L);
        when(review.getContent()).thenReturn("사용자 리뷰");
        PageRequest pageable = PageRequest.of(0, 20);
        when(reviewRepository.findByUserGame_User_Id(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        var response = reviewService.getUserReviews(USER_ID, pageable);

        assertThat(response.reviews()).extracting(item -> item.reviewId())
                .containsExactly(10L);
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @DisplayName("게임 기록이 없으면 내 상세 조회 결과가 모두 null이다")
    @Test
    void getMyDetailedReviewWithoutUserGame() {
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID))
                .thenReturn(Optional.empty());

        DetailedReviewResponse response = reviewService.getMyDetailedReview(USER_ID, GAME_ID);

        assertThat(response.userGame()).isNull();
        assertThat(response.review()).isNull();
        verify(reviewRepository, never()).findByUserGame_Id(any());
    }

    @DisplayName("게임 기록만 있으면 내 상세 조회의 리뷰는 null이다")
    @Test
    void getMyDetailedReviewWithOnlyUserGame() {
        prepareOwnedUserGame();
        when(userGame.getGame()).thenReturn(game);
        when(game.getId()).thenReturn(GAME_ID);
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID))
                .thenReturn(Optional.of(userGame));
        when(reviewRepository.findByUserGame_Id(USER_GAME_ID)).thenReturn(Optional.empty());

        DetailedReviewResponse response = reviewService.getMyDetailedReview(USER_ID, GAME_ID);

        assertThat(response.userGame()).isNotNull();
        assertThat(response.userGame().id()).isEqualTo(USER_GAME_ID);
        assertThat(response.review()).isNull();
    }

    @DisplayName("게임 기록과 리뷰가 모두 있으면 내 상세 조회에서 함께 반환한다")
    @Test
    void getMyDetailedReviewWithReview() {
        prepareOwnedUserGame();
        when(userGame.getGame()).thenReturn(game);
        when(game.getId()).thenReturn(GAME_ID);
        Review review = prepareReview();
        when(review.getId()).thenReturn(10L);
        when(review.getContent()).thenReturn("내 상세 리뷰");
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID))
                .thenReturn(Optional.of(userGame));
        when(reviewRepository.findByUserGame_Id(USER_GAME_ID))
                .thenReturn(Optional.of(review));

        DetailedReviewResponse response = reviewService.getMyDetailedReview(USER_ID, GAME_ID);

        assertThat(response.userGame().id()).isEqualTo(USER_GAME_ID);
        assertThat(response.review().reviewId()).isEqualTo(10L);
        assertThat(response.review().content()).isEqualTo("내 상세 리뷰");
    }

    private void prepareUserGameSave() {
        when(userGame.getId()).thenReturn(USER_GAME_ID);
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(userGame.getGame()).thenReturn(game);
        when(game.getId()).thenReturn(GAME_ID);
        when(userGame.isBacklog()).thenReturn(true);
        when(userGame.isInLibrary()).thenReturn(true);
        when(userGameService.addOrUpdateGameToLibrary(
                USER_ID,
                GAME_ID,
                userGameRequest
        )).thenReturn(new UserGameSaveResult(userGame, true));
    }

    private void prepareOwnedUserGame() {
        lenient().when(userGame.getId()).thenReturn(USER_GAME_ID);
        when(userGame.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
    }

    private Review prepareReview() {
        Review review = org.mockito.Mockito.mock(Review.class);
        when(review.getUserGame()).thenReturn(userGame);
        return review;
    }
}
