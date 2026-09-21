package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.GameGenre;
import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER")
@ActiveProfiles("test")
@Transactional
class RelatedGameRecommendationTest {

    @Autowired
    private GameDetailService gameDetailService;

    @Autowired
    private EntityManager entityManager;

    private Game baseGame;
    private Genre rpg;

    @BeforeEach
    void setUp() {
        baseGame = saveGame(1000L, "기준 게임", "90.0");
        rpg = Genre.createFromIgdb(1000L, "RPG");
        entityManager.persist(rpg);
        entityManager.persist(new GameGenre(baseGame, rpg));
    }

    @Test
    @DisplayName("기준 게임의 긍정 사용자만 후보의 별점과 좋아요에 점수를 더한다")
    void getRelatedGamesScoresOnlyPositiveUsersInteractions() {
        // given: 긍정 사용자는 B에 별점과 좋아요를 남기고 C는 플레이만 한 상태
        Game b = saveCandidate(1001L, "B", "80.0");
        Game c = saveCandidate(1002L, "C", "80.0");
        User positiveUser = saveUser(1);
        saveUserGame(positiveUser, baseGame, PlayStatus.COMPLETED, false);
        saveReview(saveUserGame(positiveUser, b, null, true), "4.5");
        saveUserGame(positiveUser, c, PlayStatus.PLAYED, false);

        User otherUser = saveUser(2);
        saveUserGame(otherUser, baseGame, null, false);
        saveReview(saveUserGame(otherUser, c, null, true), "5.0");
        entityManager.flush();

        // when: 기준 게임의 연관 추천을 조회
        List<RelatedGameResponse> result = gameDetailService.getRelatedGames(baseGame.getId());

        // then: B는 별점 1.5점과 좋아요 1점이 더해지고 C는 기본 점수만 받는다
        assertThat(result).extracting(RelatedGameResponse::id)
                .containsExactly(b.getId(), c.getId());
        assertThat(result.get(0).recommendationScore()).isEqualByComparingTo("57.5");
        assertThat(result.get(1).recommendationScore()).isEqualByComparingTo("55.0");
    }

    @Test
    @DisplayName("공유 장르와 IGDB 평점으로 후보를 제한하고 최종 점수 상위 5개를 반환한다")
    void getRelatedGamesReturnsTopFiveAfterFinalScoring() {
        // given: 동일 점수 후보 5개와 공유 장르가 하나 더 많은 후보가 있을 때
        List<Game> candidates = List.of(
                saveCandidate(1011L, "후보 1", "80.0"),
                saveCandidate(1012L, "후보 2", "80.0"),
                saveCandidate(1013L, "후보 3", "80.0"),
                saveCandidate(1014L, "후보 4", "80.0"),
                saveCandidate(1015L, "후보 5", "80.0")
        );
        Game twoGenres = saveCandidate(1016L, "장르 2개", "80.0");
        Genre action = Genre.createFromIgdb(1001L, "액션");
        entityManager.persist(action);
        entityManager.persist(new GameGenre(baseGame, action));
        entityManager.persist(new GameGenre(twoGenres, action));

        saveCandidate(1017L, "IGDB 평점 미달", "69.0");
        saveGame(1018L, "공유 장르 없음", "99.0");
        entityManager.flush();

        // when: 기준 게임의 연관 추천을 조회
        List<RelatedGameResponse> result = gameDetailService.getRelatedGames(baseGame.getId());

        // then: 장르 가산점이 적용된 후보가 먼저 나오고 동점은 ID순으로 4개만 나온다
        assertThat(result).extracting(RelatedGameResponse::id)
                .containsExactly(
                        twoGenres.getId(),
                        candidates.get(0).getId(),
                        candidates.get(1).getId(),
                        candidates.get(2).getId(),
                        candidates.get(3).getId()
                );
        assertThat(result.get(0).recommendationScore()).isEqualByComparingTo("70.0");
        assertThat(result.get(0).genres())
                .extracting(GameDetailResponse.GenreResponse::name)
                .containsExactlyInAnyOrder("RPG", "액션");
        assertThat(result.get(1).genres())
                .extracting(GameDetailResponse.GenreResponse::name)
                .containsExactly("RPG");
    }

    @Test
    @DisplayName("별점 5개 미만은 포함하고 5개 이상이면 평균 3.5 이상만 포함한다")
    void getRelatedGamesAppliesActualRatingFilter() {
        // given: 후보마다 실제 별점 개수와 평균이 다를 때
        Game fewRatings = saveCandidate(1021L, "별점 4개", "80.0");
        Game lowAverage = saveCandidate(1022L, "낮은 평균", "80.0");
        Game highAverage = saveCandidate(1023L, "높은 평균", "80.0");
        for (int i = 1; i <= 5; i++) {
            if (i <= 4) {
                saveReview(saveUserGame(saveUser(i), fewRatings, null, false), "1.0");
            }
            saveReview(saveUserGame(saveUser(i + 10), lowAverage, null, false), "1.0");
            saveReview(saveUserGame(saveUser(i + 20), highAverage, null, false), "4.0");
        }
        UserGame contentOnly = saveUserGame(saveUser(40), fewRatings, null, false);
        entityManager.persist(new Review(contentOnly, null, "별점 없는 리뷰", false));
        entityManager.flush();

        // when: 기준 게임의 연관 추천을 조회
        List<RelatedGameResponse> result = gameDetailService.getRelatedGames(baseGame.getId());

        // then: 별점 없는 리뷰는 개수에서 빠지고 평균 1점인 별점 5개 후보는 제외된다
        assertThat(result).extracting(RelatedGameResponse::id)
                .containsExactly(fewRatings.getId(), highAverage.getId());
    }

    @Test
    @DisplayName("존재하지 않는 기준 게임의 연관 추천 조회는 실패한다")
    void getRelatedGamesThrowsWhenBaseGameDoesNotExist() {
        // given: 저장되지 않은 게임 ID
        Long missingGameId = Long.MAX_VALUE;

        // when: 연관 추천을 조회
        Throwable throwable = catchThrowable(() -> gameDetailService.getRelatedGames(missingGameId));

        // then: 게임이 없다는 예외가 발생한다
        assertThat(throwable)
                .isInstanceOf(ServiceException.class)
                .hasMessage("존재하지 않는 게임입니다.");
    }

    private Game saveCandidate(Long igdbId, String title, String igdbRating) {
        Game candidate = saveGame(igdbId, title, igdbRating);
        entityManager.persist(new GameGenre(candidate, rpg));
        return candidate;
    }

    private Game saveGame(Long igdbId, String title, String igdbRating) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(
                igdbId, title, null, null, null, new BigDecimal(igdbRating),
                List.of(), List.of(), List.of(), List.of()
        ));
        entityManager.persist(game);
        return game;
    }

    private User saveUser(int number) {
        User user = new User("related" + number, "related" + number + "@test.com", "password");
        entityManager.persist(user);
        return user;
    }

    private UserGame saveUserGame(User user, Game game, PlayStatus status, boolean liked) {
        UserGame userGame = new UserGame(user, game);
        if (status != null) {
            userGame.changePlayStatus(status);
        }
        userGame.changeLiked(liked);
        entityManager.persist(userGame);
        return userGame;
    }

    private void saveReview(UserGame userGame, String rating) {
        entityManager.persist(new Review(userGame, new BigDecimal(rating), null, false));
    }
}
