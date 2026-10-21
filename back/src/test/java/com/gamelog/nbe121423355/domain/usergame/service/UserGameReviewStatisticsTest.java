package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.support.MySqlIntegrationTest;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.review.service.GameRecordReviewService;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@MySqlIntegrationTest
@Transactional
class UserGameReviewStatisticsTest {
    @Autowired UserGameService library;
    @Autowired GameRecordReviewService gameRecordReviewService;
    @Autowired UserRepository users;
    @Autowired GameRepository games;
    @Autowired ReviewRepository reviews;
    @Autowired UserGameRepository records;
    @Autowired EntityManager em;
    Long userId;
    Genre genre;
    long nextGameId = 991000L;

    @BeforeEach
    void setUp() {
        userId = users.save(new User("review-stats", "review-stats@example.com", "password")).getId();
        genre = Genre.createFromIgdb(991000L, "통계 테스트 장르");
        em.persist(genre);
    }

    private UserGame save(PlayStatus status, String hours, String rating, String content) {
        Game game = games.save(Game.createFromIgdb(new IgdbGameResponse(nextGameId++, "game-" + nextGameId, "test",
                null, null, null, List.of(), List.of(), List.of(), List.of())));
        em.persist(new GameGenre(game, genre));
        var record = new UserGameReqBody(status, false, false, false, false, null,
                hours == null ? null : new BigDecimal(hours), null, null, null, null, null);
        var review = rating == null && content == null ? null
                : new ReviewSaveRequest(rating == null ? null : new BigDecimal(rating), content, false);
        var response = gameRecordReviewService.saveDetailedReview(userId, game.getId(), new DetailedReviewSaveRequest(record, review));
        return records.findById(response.userGame().id()).orElseThrow();
    }

    private Review review(UserGame game) { return reviews.findByUserGame_Id(game.getId()).orElseThrow(); }

    @Test
    @DisplayName("플레이 상태 없이 별점이나 양수 시간이 있어도 집계하고 본문만 있는 게임과 0시간은 제외한다")
    void statisticsUseRatingsAndPositiveHoursWithoutPlayStatus() {
        UserGame rated = save(null, null, "4.0", null);
        UserGame timed = save(null, "10.0", null, null);
        UserGame both = save(null, "5.0", "2.0", null);
        UserGame contentOnly = save(null, null, null, "기대평");
        UserGame zeroOnly = save(null, "0.0", null, null);
        UserGame zeroRated = save(null, "0.0", "3.0", null);
        UserGame hidden = save(null, "20.0", "5.0", null);
        review(hidden).hideByAdmin();
        library.synchronizeLibraryState(hidden);
        UserGame deleted = save(null, "30.0", "5.0", null);
        gameRecordReviewService.deleteReview(userId, review(deleted).getId());
        UserGame completed = save(PlayStatus.COMPLETED, null, null, null);
        UserGame wish = save(null, null, null, null);
        library.changeWishlist(userId, wish.getGame().getId(), true);
        List<Long> expectedPlayed = List.of(rated.getId(), timed.getId(), both.getId(), zeroRated.getId(), hidden.getId(), deleted.getId(), completed.getId());
        Long scatterGameId = both.getGame().getId();
        em.flush();
        em.clear();

        var profile = library.profileTab(userId, userId);
        assertThat(profile.stats().playedGameCount()).isEqualTo(7);
        assertThat(profile.stats().averageRating()).isEqualByComparingTo("3.0");
        assertThat(profile.stats().totalPlayTime()).isEqualByComparingTo("65.0");
        assertThat(profile.scatterData()).extracting(UserGameScatterDto::gameId).containsExactly(scatterGameId);
        assertThat(records.findPlayedGames(userId)).extracting(UserGame::getId).containsExactlyInAnyOrderElementsOf(expectedPlayed);
        assertThat(records.findGenreDistribution(userId)).singleElement().satisfies(g -> assertThat(g.gameCount()).isEqualTo(7));
        assertThat(profile.tasteResponse().completion().ratio()).isEqualByComparingTo("0.1429");
        assertThat(profile.tasteResponse().longPlay().ratio()).isEqualByComparingTo("0.75");
        assertThat(records.findById(contentOnly.getId()).orElseThrow().isInLibrary()).isTrue();
        assertThat(records.findById(zeroOnly.getId()).orElseThrow().isInLibrary()).isFalse();
    }

    @Test
    @DisplayName("삭제·숨김 리뷰의 별점은 정렬에서 제외하되 게임과 페이지 전체 건수는 유지한다")
    void ratingSortKeepsGamesWithInvisibleOrMissingReviews() {
        UserGame active = save(null, null, "2.0", null);
        UserGame deleted = save(null, "10", "5.0", null);
        gameRecordReviewService.deleteReview(userId, review(deleted).getId());
        UserGame hidden = save(null, "5", "4.0", null);
        review(hidden).hideByAdmin();
        library.synchronizeLibraryState(hidden);
        UserGame unrated = save(null, "7", null, null);
        List<Long> expectedGames = List.of(active.getGame().getId(), deleted.getGame().getId(), hidden.getGame().getId(), unrated.getGame().getId());
        Long firstGame = active.getGame().getId();
        em.flush();
        em.clear();
        UserGameSearchRequest request = new UserGameSearchRequest();
        request.setSort(UserGameSort.RATING);
        request.setSize(1);
        var firstPage = library.getUserGameList(userId, request);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getContent()).extracting(UserGameListResponse::gameId).containsExactly(firstGame);
        request.setSize(60);
        assertThat(library.getUserGameList(userId, request).getContent()).extracting(UserGameListResponse::gameId)
                .containsExactlyInAnyOrderElementsOf(expectedGames);
    }

    @Test
    @DisplayName("status가 null인 기존 리뷰도 라이브러리·정렬·평균·산점도에서 활성 리뷰로 취급한다")
    void legacyNullStatusRemainsActive() {
        UserGame legacy = save(null, "5", "4.0", null);
        save(null, null, "2.0", null);
        Long recordId = legacy.getId(), gameId = legacy.getGame().getId();
        em.flush();
        em.createQuery("UPDATE Review r SET r.status = NULL WHERE r.userGame.id = :id")
                .setParameter("id", recordId).executeUpdate();
        em.clear();
        library.changeLiked(userId, gameId, true);
        assertThat(records.findById(recordId).orElseThrow().isInLibrary()).isTrue();
        var profile = library.profileTab(userId, userId);
        assertThat(profile.stats().playedGameCount()).isEqualTo(2);
        assertThat(profile.stats().averageRating()).isEqualByComparingTo("3.0");
        assertThat(profile.scatterData()).extracting(UserGameScatterDto::gameId).containsExactly(gameId);
        UserGameSearchRequest request = new UserGameSearchRequest();
        request.setSort(UserGameSort.RATING);
        assertThat(library.getUserGameList(userId, request).getContent().getFirst().gameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("활성 별점을 본문만 있는 리뷰로 바꾸면 라이브러리는 유지하고 플레이 통계에서는 제외한다")
    void contentOnlyAfterEditIsNotPlayed() {
        UserGame rated = save(null, null, "4.0", null);
        gameRecordReviewService.updateReview(userId, review(rated).getId(), new ReviewSaveRequest(null, "본문만 남김", false));
        em.flush();
        em.clear();
        var profile = library.profileTab(userId, userId);
        assertThat(profile.stats().playedGameCount()).isZero();
        assertThat(profile.stats().averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(profile.scatterData()).isEmpty();
        assertThat(library.getUserGameList(userId, new UserGameSearchRequest()).getTotalElements()).isEqualTo(1);
    }
}
