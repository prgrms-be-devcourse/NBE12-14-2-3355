package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    @Test
    @DisplayName("페이지 조회는 저장된 게임의 DTO 필드를 반환한다")
    void getGamesPageMapsDtoFields() {
        // given
        assertThat(gameRepository.count()).isZero();

        Game firstGame = gameRepository.save(
                createGame(1001L, "첫 번째 게임")
        );
        Game secondGame = gameRepository.save(
                createGame(1002L, "두 번째 게임")
        );

        // when
        List<GameListResponse> responses = gameService.getGamesPage(new GameSearchRequest(0, 20, null, null, null, null)).getContent();

        // then
        assertThat(responses)
                .extracting(
                        GameListResponse::id,
                        GameListResponse::title,
                        GameListResponse::coverImageUrl,
                        GameListResponse::releaseDate,
                        GameListResponse::igdbRating
                )
                .containsExactly(
                        tuple(
                                firstGame.getId(),
                                "첫 번째 게임",
                                "https://images.igdb.com/test.jpg",
                                LocalDate.of(2024, 1, 1),
                                new BigDecimal("90.50")
                        ),
                        tuple(
                                secondGame.getId(),
                                "두 번째 게임",
                                "https://images.igdb.com/test.jpg",
                                LocalDate.of(2024, 1, 1),
                                new BigDecimal("90.50")
                        )
                );
    }

    @Test
    @DisplayName("페이지 내용은 저장된 게임이 없으면 빈 목록이다")
    void getGamesPageContentIsEmptyWhenNoGamesExist() {
        // given
        assertThat(gameRepository.count()).isZero();

        // when
        List<GameListResponse> responses = gameService.getGamesPage(new GameSearchRequest(0, 20, null, null, null, null)).getContent();

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("페이징 조회는 게임 3개를 2개씩 두 페이지로 반환한다")
    void getGamesPageReturnsPages() {
        // given
        assertThat(gameRepository.count()).isZero();

        gameRepository.save(createGame(1001L, "첫 번째 게임"));
        gameRepository.save(createGame(1002L, "두 번째 게임"));
        gameRepository.save(createGame(1003L, "세 번째 게임"));

        // when
        Page<GameListResponse> firstPage = gameService.getGamesPage(
                new GameSearchRequest(0, 2, null, null, null, null)
        );

        Page<GameListResponse> secondPage = gameService.getGamesPage(
                new GameSearchRequest(1, 2, null, null, null, null)
        );

        // then
        assertThat(firstPage.getContent())
                .extracting(GameListResponse::title)
                .containsExactly("첫 번째 게임", "두 번째 게임");

        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.getContent())
                .extracting(GameListResponse::title)
                .containsExactly("세 번째 게임");

        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.isLast()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("페이징 조회 시 저장된 게임이 없으면 빈 페이지를 반환한다")
    void getGamesPageReturnsEmptyPageWhenNoGamesExist() {
        // given
        assertThat(gameRepository.count()).isZero();

        GameSearchRequest request = new GameSearchRequest(0, 20, null, null, null, null);

        // when
        Page<GameListResponse> responses =
                gameService.getGamesPage(request);

        // then
        assertThat(responses.getContent()).isEmpty();
        assertThat(responses.getTotalElements()).isZero();
        assertThat(responses.getTotalPages()).isZero();
        assertThat(responses.hasNext()).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지를 초과하면 목록은 비어 있고 전체 개수는 유지된다")
    void getGamesPageReturnsEmptyContentWhenPageExceedsLastPage() {
        // given
        assertThat(gameRepository.count()).isZero();

        gameRepository.save(createGame(1001L, "첫 번째 게임"));
        gameRepository.save(createGame(1002L, "두 번째 게임"));
        gameRepository.save(createGame(1003L, "세 번째 게임"));

        // when
        Page<GameListResponse> responses = gameService.getGamesPage(
                new GameSearchRequest(2, 2, null, null, null, null)
        );

        // then
        assertThat(responses.getContent()).isEmpty();
        assertThat(responses.getNumber()).isEqualTo(2);
        assertThat(responses.getTotalElements()).isEqualTo(3);
        assertThat(responses.getTotalPages()).isEqualTo(2);
        assertThat(responses.hasNext()).isFalse();
    }

    @Test
    @DisplayName("제목 검색은 대소문자와 앞뒤 공백을 무시하고 검색 결과를 페이징한다")
    void searchGamesReturnsMatchingPage() {
        assertThat(gameRepository.count()).isZero();
        gameRepository.save(createGame(1001L, "The Legend of Zelda"));
        gameRepository.save(createGame(1002L, "ZELDA Adventure"));
        gameRepository.save(createGame(1003L, "Super Mario"));

        Page<GameListResponse> firstPage = gameService.getGamesPage(
                new GameSearchRequest(0, 1, null, " zelda ", null, null)
        );
        Page<GameListResponse> secondPage = gameService.getGamesPage(
                new GameSearchRequest(1, 1, null, " zelda ", null, null)
        );

        assertThat(firstPage.getContent()).extracting(GameListResponse::title)
                .containsExactly("The Legend of Zelda");
        assertThat(secondPage.getContent()).extracting(GameListResponse::title)
                .containsExactly("ZELDA Adventure");
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("공백 검색어는 전체 게임을 반환한다")
    void searchGamesWithBlankKeywordReturnsAllGames() {
        assertThat(gameRepository.count()).isZero();
        gameRepository.save(createGame(1001L, "Zelda"));
        gameRepository.save(createGame(1002L, "Super Mario"));

        Page<GameListResponse> result = gameService.getGamesPage(
                new GameSearchRequest(0, 20, null, "   ", null, null)
        );

        assertThat(result.getContent()).extracting(GameListResponse::title)
                .containsExactly("Zelda", "Super Mario");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
    void searchGamesWithNoMatchReturnsEmptyPage() {
        assertThat(gameRepository.count()).isZero();
        gameRepository.save(createGame(1001L, "Super Mario"));

        Page<GameListResponse> result = gameService.getGamesPage(
                new GameSearchRequest(0, 20, null, "zelda", null, null)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    private Game createGame(Long igdbId, String title) {
        IgdbGameResponse response = new IgdbGameResponse(
                igdbId,
                title,
                "테스트 게임 설명",
                new IgdbGameResponse.Cover(
                        1L,
                        "//images.igdb.com/test.jpg"
                ),
                1704067200L,
                new BigDecimal("90.50"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        return Game.createFromIgdb(response);
    }
}
