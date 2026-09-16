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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    @DisplayName("전체 목록 조회는 저장된 게임을 DTO로 모두 반환한다")
    void getGamesReturnsAllGames() {
        // given
        assertThat(gameRepository.count()).isZero();

        Game firstGame = gameRepository.save(
                createGame(1001L, "첫 번째 게임")
        );
        Game secondGame = gameRepository.save(
                createGame(1002L, "두 번째 게임")
        );

        // when
        List<GameListResponse> responses = gameService.getGames();

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
    @DisplayName("전체 목록 조회 시 저장된 게임이 없으면 빈 목록을 반환한다")
    void getGamesReturnsEmptyListWhenNoGamesExist() {
        // given
        assertThat(gameRepository.count()).isZero();

        // when
        List<GameListResponse> responses = gameService.getGames();

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
                PageRequest.of(0, 2, Sort.by("id"))
        );

        Page<GameListResponse> secondPage = gameService.getGamesPage(
                PageRequest.of(1, 2, Sort.by("id"))
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

        PageRequest pageable = PageRequest.of(0, 20, Sort.by("id"));

        // when
        Page<GameListResponse> responses =
                gameService.getGamesPage(pageable);

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
                PageRequest.of(2, 2, Sort.by("id"))
        );

        // then
        assertThat(responses.getContent()).isEmpty();
        assertThat(responses.getNumber()).isEqualTo(2);
        assertThat(responses.getTotalElements()).isEqualTo(3);
        assertThat(responses.getTotalPages()).isEqualTo(2);
        assertThat(responses.hasNext()).isFalse();
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
