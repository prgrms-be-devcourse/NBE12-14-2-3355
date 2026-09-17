package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameRatingDistributionResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER"
)
@ActiveProfiles("test")
@Transactional
class GameDetailServiceTest {

    @Autowired
    private GameDetailService gameDetailService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private GameSeriesRepository gameSeriesRepository;

    @Autowired
    private GameGenreRepository gameGenreRepository;

    @Autowired
    private GamePlatformRepository gamePlatformRepository;

    @Autowired
    private GameSeriesGameRepository gameSeriesGameRepository;

    @Autowired
    private EntityManager entityManager;

    private Game game;
    private Long gameId;

    @BeforeEach
    void setUp() {
        IgdbGameResponse igdbGame = new IgdbGameResponse(
                1942L,
                "The Witcher 3: Wild Hunt",
                "오픈 월드 액션 RPG",
                new IgdbGameResponse.Cover(
                        480513L,
                        "//images.igdb.com/igdb/image/upload/t_thumb/test.jpg"
                ),
                1431993600L,
                new BigDecimal("93.78"),
                List.of(
                        new IgdbGameResponse.InvolvedCompany(
                                1L,
                                true,
                                new IgdbGameResponse.Company(
                                        1L,
                                        "CD Projekt RED"
                                )
                        )
                ),
                List.of(),
                List.of(),
                List.of()
        );

        game = gameRepository.save(
                Game.createFromIgdb(igdbGame)
        );

        Genre genre = genreRepository.save(
                Genre.createFromIgdb(12L, "RPG")
        );

        Platform platform = platformRepository.save(
                Platform.createFromIgdb(6L, "PC")
        );

        GameSeries series = gameSeriesRepository.save(
                GameSeries.createFromIgdb(62L, "The Witcher")
        );

        gameGenreRepository.save(
                new GameGenre(game, genre)
        );

        gamePlatformRepository.save(
                new GamePlatform(game, platform)
        );

        gameSeriesGameRepository.save(
                new GameSeriesGame(series, game)
        );

        gameId = game.getId();
    }

    @DisplayName("게임 기본 정보와 연결 정보를 모두 조회한다")
    @Test
    void getGameDetailReturnsGameInformation() {
        // given: 게임 기본 정보와 장르·플랫폼·시리즈가 저장되어 있을 때
        Long savedGameId = gameId;

        // when: 저장된 게임 ID로 상세 정보를 조회하면
        GameDetailResponse response =
                gameDetailService.getGameDetail(savedGameId);

        // then: 게임 기본 정보와 연결 정보 및 0으로 초기화된 통계가 반환된다
        assertThat(response.id()).isEqualTo(gameId);
        assertThat(response.igdbId()).isEqualTo(1942L);
        assertThat(response.title())
                .isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(response.developer())
                .isEqualTo("CD Projekt RED");
        assertThat(response.igdbRating())
                .isEqualByComparingTo("93.78");

        assertThat(response.genres())
                .extracting(GameDetailResponse.GenreResponse::name)
                .containsExactly("RPG");

        assertThat(response.platforms())
                .extracting(GameDetailResponse.PlatformResponse::name)
                .containsExactly("PC");

        assertThat(response.series())
                .extracting(GameDetailResponse.SeriesResponse::name)
                .containsExactly("The Witcher");

        assertThat(response.statistics().playedCount()).isZero();
        assertThat(response.statistics().playingCount()).isZero();
        assertThat(response.statistics().backlogCount()).isZero();
        assertThat(response.statistics().wishlistCount()).isZero();
        assertThat(response.statistics().likeCount()).isZero();
        assertThat(response.statistics().averageRating())
                .isEqualByComparingTo("0");
        assertThat(response.statistics().reviewCount()).isZero();
        assertThat(response.statistics().ratingDistribution())
                .hasSize(10)
                .allSatisfy(item -> assertThat(item.count()).isZero());
    }

    @DisplayName("게임의 상태별 사용자 수를 조회한다")
    @Test
    void getGameDetailReturnsGameStatistics() {
        // given: 서로 다른 상태로 게임을 등록한 사용자들이 있을 때
        saveUserGame(
                "user1@test.com",
                "user1",
                PlayStatus.COMPLETED,
                true,
                false,
                false
        );

        saveUserGame(
                "user2@test.com",
                "user2",
                PlayStatus.PLAYED,
                false,
                true,
                false
        );

        saveUserGame(
                "user3@test.com",
                "user3",
                null,
                false,
                false,
                true
        );

        entityManager.flush();

        // when: 해당 게임의 상세 정보를 조회하면
        GameDetailResponse response =
                gameDetailService.getGameDetail(gameId);

        // then: Played, Playing, Backlog, Wishlist 인원수가 상태별로 집계된다
        assertThat(response.statistics().playedCount()).isEqualTo(2L);
        assertThat(response.statistics().playingCount()).isEqualTo(1L);
        assertThat(response.statistics().backlogCount()).isEqualTo(1L);
        assertThat(response.statistics().wishlistCount()).isEqualTo(1L);
    }

    @DisplayName("게임의 평점, 리뷰 수, 좋아요 수와 평점별 분포를 조회한다")
    @Test
    void getGameDetailReturnsRatingReviewAndLikeStatistics() {
        // given: 평점과 좋아요 상태가 서로 다른 사용자들이 있을 때
        UserGame halfRating = saveUserGame(
                "rating1@test.com", "rating1",
                null, false, false, false
        );
        ReflectionTestUtils.setField(halfRating, "liked", true);
        entityManager.persist(new Review(
                halfRating, new BigDecimal("0.5"), null, false
        ));

        UserGame fiveRatingWithoutLike = saveUserGame(
                "rating2@test.com", "rating2",
                null, false, false, false
        );
        entityManager.persist(new Review(
                fiveRatingWithoutLike, new BigDecimal("5.0"), null, false
        ));

        UserGame fiveRatingWithLike = saveUserGame(
                "rating3@test.com", "rating3",
                null, false, false, false
        );
        ReflectionTestUtils.setField(fiveRatingWithLike, "liked", true);
        entityManager.persist(new Review(
                fiveRatingWithLike, new BigDecimal("5.0"), null, false
        ));

        UserGame contentOnlyReview = saveUserGame(
                "rating-content@test.com", "ratingContent",
                null, false, false, false
        );
        entityManager.persist(new Review(
                contentOnlyReview, null, "별점 없이 작성한 리뷰", false
        ));

        UserGame likedWithoutReview = saveUserGame(
                "rating4@test.com", "rating4",
                null, false, false, false
        );
        ReflectionTestUtils.setField(likedWithoutReview, "liked", true);

        entityManager.flush();

        // when: 해당 게임의 상세 정보를 조회하면
        GameDetailResponse response = gameDetailService.getGameDetail(gameId);

        // then: 글만 작성한 리뷰도 리뷰 수에는 포함하고 평점 통계에서는 제외한다
        assertThat(response.statistics().averageRating())
                .isEqualByComparingTo("3.5");
        assertThat(response.statistics().reviewCount()).isEqualTo(4L);
        assertThat(response.statistics().likeCount()).isEqualTo(3L);

        List<GameRatingDistributionResponse> distribution =
                response.statistics().ratingDistribution();

        assertThat(distribution).hasSize(10);
        assertThat(distribution.get(0).rating())
                .isEqualByComparingTo("0.5");
        assertThat(distribution.get(9).rating())
                .isEqualByComparingTo("5.0");
        assertThat(distribution)
                .extracting(GameRatingDistributionResponse::count)
                .containsExactly(
                        1L, 0L, 0L, 0L, 0L,
                        0L, 0L, 0L, 0L, 2L
                );
    }

    @DisplayName("존재하지 않는 게임 ID를 조회하면 예외가 발생한다")
    @Test
    void getGameDetailThrowsExceptionWhenGameDoesNotExist() {
        // given: 존재하지 않는 게임 ID가 주어졌을 때
        Long nonexistentGameId = Long.MAX_VALUE;

        // when: 해당 게임의 상세 정보를 조회하면
        Throwable throwable = catchThrowable(
                () -> gameDetailService.getGameDetail(nonexistentGameId)
        );

        // then: 게임이 존재하지 않는다는 ServiceException이 발생한다
        assertThat(throwable)
                .isInstanceOf(ServiceException.class)
                .hasMessage("존재하지 않는 게임입니다.");
    }

    private UserGame saveUserGame(
            String email,
            String nickname,
            PlayStatus playStatus,
            boolean playing,
            boolean backlog,
            boolean wishlist
    ) {
        User user = new User(
                null,
                email,
                "password",
                nickname,
                null,
                null,
                false,
                "USER"
        );

        entityManager.persist(user);

        UserGame userGame = new UserGame(user, game);

        ReflectionTestUtils.setField(
                userGame,
                "playStatus",
                playStatus
        );
        ReflectionTestUtils.setField(
                userGame,
                "playing",
                playing
        );
        ReflectionTestUtils.setField(
                userGame,
                "backlog",
                backlog
        );
        ReflectionTestUtils.setField(
                userGame,
                "wishlist",
                wishlist
        );

        entityManager.persist(userGame);
        return userGame;
    }

}
