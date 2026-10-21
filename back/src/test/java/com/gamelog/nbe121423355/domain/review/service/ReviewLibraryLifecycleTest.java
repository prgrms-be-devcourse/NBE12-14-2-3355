package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.support.MySqlIntegrationTest;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.review.dto.request.*;
import com.gamelog.nbe121423355.domain.review.entity.ReportStatus;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSearchRequest;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@MySqlIntegrationTest
@Transactional
class ReviewLibraryLifecycleTest {
    @Autowired GameRecordReviewService gameRecordReviewService;
    @Autowired ReviewReportService reports;
    @Autowired UserGameService library;
    @Autowired UserRepository users;
    @Autowired GameRepository games;
    @Autowired ReviewRepository reviews;
    @Autowired UserGameRepository records;
    @Autowired UserFavoriteGameRepository favorites;
    @Autowired EntityManager em;
    Long userId;
    Long gameId;

    @BeforeEach
    void setUp() {
        userId = users.save(new User("review-library", "review-library@example.com", "password")).getId();
        gameId = games.save(Game.createFromIgdb(new IgdbGameResponse(990001L, "review-library", "test",
                null, null, null, List.of(), List.of(), List.of(), List.of()))).getId();
    }

    private UserGameReqBody record(boolean playing) {
        return new UserGameReqBody(null, playing, false, false, true,
                null, null, null, null, null, null, null);
    }

    private ReviewSaveRequest rating() { return new ReviewSaveRequest(new BigDecimal("4.0"), null, false); }

    private UserGame reload() {
        em.flush();
        em.clear();
        return records.findByUser_IdAndGame_Id(userId, gameId).orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("별점만 또는 본문만 작성해도 라이브러리에 포함되고 응답에도 반영된다")
    void reviewOnlyIsIncluded(boolean ratingOnly) {
        var request = ratingOnly ? rating() : new ReviewSaveRequest(null, "본문만 작성", false);
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(false), request));
        assertThat(response.userGame().inLibrary()).isTrue();
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(library.getUserGameList(userId, new UserGameSearchRequest()).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("리뷰만 있는 게임의 리뷰를 삭제하면 라이브러리와 인생게임에서 제외되고 재작성 시 복원된다")
    void deleteAndRestoreReviewOnly() {
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(false), rating()));
        Long recordId = response.userGame().id(), reviewId = response.review().reviewId();
        library.updateFavoriteGames(userId, List.of(gameId));
        gameRecordReviewService.deleteReview(userId, reviewId);
        UserGame saved = reload();
        assertThat(saved.isInLibrary()).isFalse();
        assertThat(saved.isLiked()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).isEmpty();
        assertThat(reviews.findById(reviewId).orElseThrow().getRating()).isEqualByComparingTo("4.0");
        var restored = gameRecordReviewService.saveReview(userId, recordId, rating());
        assertThat(restored.reviewId()).isEqualTo(reviewId);
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("신고 승인으로 리뷰가 숨겨지면 다른 플레이 기록의 유무에 따라 라이브러리와 인생게임을 갱신한다")
    void hidingReviewRecalculatesMembership(boolean playing) {
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(playing), rating()));
        library.updateFavoriteGames(userId, List.of(gameId));
        Long reporterId = users.save(new User("reporter", "reporter@example.com", "password")).getId();
        var report = reports.createReport(reporterId, response.review().reviewId(), new ReviewReportCreateRequest("신고 테스트"));
        reports.updateStatus(report.reportId(), new ReviewReportStatusUpdateRequest(ReportStatus.APPROVED));
        assertThat(reload().isInLibrary()).isEqualTo(playing);
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).hasSize(playing ? 1 : 0);
        assertThat(reviews.findById(response.review().reviewId()).orElseThrow().getStatus()).isEqualTo(ReviewStatus.HIDDEN_BY_ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"played", "playing", "wishlist", "backlog", "liked", "record"})
    @DisplayName("게임 상태나 기록을 지워도 활성 리뷰가 남으면 라이브러리와 인생게임이 유지된다")
    void activeReviewSurvivesGameRecordChanges(String route) {
        gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(true), rating()));
        library.updateFavoriteGames(userId, List.of(gameId));
        library.changePlaying(userId, gameId, false);
        switch (route) {
            case "played" -> library.changePlayed(userId, gameId, null);
            case "playing" -> library.changePlaying(userId, gameId, false);
            case "wishlist" -> library.changeWishlist(userId, gameId, false);
            case "backlog" -> library.changeBacklog(userId, gameId, false);
            case "liked" -> library.changeLiked(userId, gameId, false);
            case "record" -> library.updatePlayRecord(userId, gameId, record(false));
        }
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).hasSize(1);
    }

    @Test
    @DisplayName("기존 기록을 모두 지우면서 새 리뷰를 작성해도 인생게임이 중간에 해제되지 않는다")
    void replacingRecordsWithNewReviewKeepsFavorite() {
        library.addOrUpdateGameToLibrary(userId, gameId, record(true));
        library.updateFavoriteGames(userId, List.of(gameId));
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(false), rating()));
        assertThat(response.userGame().inLibrary()).isTrue();
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).hasSize(1);
    }

    @Test
    @DisplayName("빈 리뷰 요청은 기존 활성 리뷰를 유지하고 게임 기록을 비워도 인생게임을 유지한다")
    void emptyReviewRequestPreservesReviewAndFavorite() {
        gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(true), rating()));
        library.updateFavoriteGames(userId, List.of(gameId));
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId,
                new DetailedReviewSaveRequest(record(false), new ReviewSaveRequest(null, " ", false)));
        assertThat(response.review().rating()).isEqualByComparingTo("4.0");
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(userId)).hasSize(1);
    }

    @Test
    @DisplayName("별점을 지우고 본문만 남겨도 라이브러리에는 유지된다")
    void editingRatingToContentOnlyKeepsLibrary() {
        var response = gameRecordReviewService.saveDetailedReview(userId, gameId, new DetailedReviewSaveRequest(record(false), rating()));
        gameRecordReviewService.updateReview(userId, response.review().reviewId(), new ReviewSaveRequest(null, "본문 유지", false));
        assertThat(reload().isInLibrary()).isTrue();
        assertThat(reviews.findById(response.review().reviewId()).orElseThrow().getRating()).isNull();
    }
}
