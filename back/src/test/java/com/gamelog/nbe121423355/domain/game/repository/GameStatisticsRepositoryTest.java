package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.projection.PopularGameProjection;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:popular-game-repository-test;MODE=MySQL;NON_KEYWORDS=USER"
)
@ActiveProfiles("test")
@Transactional
class GameStatisticsRepositoryTest {

    @Autowired
    private GameStatisticsRepository gameStatisticsRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGameRepository userGameRepository;

    @Test
    @DisplayName("좋아요 수와 게임 ID를 기준으로 인기 게임을 정렬한다")
    void findPopularGamesOrdersByLikeCountAndGameId() {
        // given: 좋아요 수가 같거나 서로 다른 게임과 좋아요가 없는 게임을 준비
        Game firstGame = saveGame(1001L, "첫 번째 게임");
        Game secondGame = saveGame(1002L, "두 번째 게임");
        Game thirdGame = saveGame(1003L, "세 번째 게임");
        Game unlikedGame = saveGame(1004L, "좋아요 없는 게임");

        User firstUser = saveUser(1);
        User secondUser = saveUser(2);
        User thirdUser = saveUser(3);

        saveUserGame(firstUser, firstGame, true);
        saveUserGame(secondUser, firstGame, true);
        saveUserGame(firstUser, secondGame, true);
        saveUserGame(secondUser, secondGame, true);
        saveUserGame(thirdUser, thirdGame, true);
        saveUserGame(thirdUser, unlikedGame, false);

        // when: 인기 게임 목록을 조회
        List<PopularGameProjection> result =
                gameStatisticsRepository.findPopularGames(
                        PageRequest.of(0, 10)
                );

        // then: 좋아요 수 내림차순과 게임 ID 내림차순으로 정렬되고 좋아요 없는 게임은 제외
        assertThat(result)
                .extracting(PopularGameProjection::getGameId)
                .containsExactly(
                        secondGame.getId(),
                        firstGame.getId(),
                        thirdGame.getId()
                );

        assertThat(result)
                .extracting(PopularGameProjection::getLikeCount)
                .containsExactly(2L, 2L, 1L);

        assertThat(result.getFirst().getTitle())
                .isEqualTo("두 번째 게임");
        assertThat(result.getFirst().getCoverImageUrl())
                .isEqualTo("https://images.igdb.com/1002.jpg");
    }

    @Test
    @DisplayName("요청한 개수만큼 인기 게임을 조회한다")
    void findPopularGamesAppliesSizeLimit() {
        // given: 좋아요가 있는 게임 두 개를 준비
        Game firstGame = saveGame(2001L, "첫 번째 게임");
        Game secondGame = saveGame(2002L, "두 번째 게임");
        User user = saveUser(10);

        saveUserGame(user, firstGame, true);
        saveUserGame(user, secondGame, true);

        // when: 조회 개수를 1개로 제한해 인기 게임을 조회
        List<PopularGameProjection> result =
                gameStatisticsRepository.findPopularGames(
                        PageRequest.of(0, 1)
                );

        // then: 게임 ID가 더 큰 게임 한 개만 반환
        assertThat(result)
                .extracting(PopularGameProjection::getGameId)
                .containsExactly(secondGame.getId());
    }

    private Game saveGame(Long igdbId, String title) {
        IgdbGameResponse response = new IgdbGameResponse(
                igdbId,
                title,
                "테스트 게임 설명",
                new IgdbGameResponse.Cover(
                        igdbId,
                        "//images.igdb.com/" + igdbId + ".jpg"
                ),
                1704067200L,
                new BigDecimal("90.50"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        return gameRepository.save(Game.createFromIgdb(response));
    }

    private User saveUser(int number) {
        return userRepository.save(new User(
                "인기게임사용자" + number,
                "popular-game-" + number + "@test.com",
                "password"
        ));
    }

    private void saveUserGame(
            User user,
            Game game,
            boolean liked
    ) {
        UserGame userGame = new UserGame(user, game);
        userGame.changeLiked(liked);
        userGameRepository.save(userGame);
    }
}
