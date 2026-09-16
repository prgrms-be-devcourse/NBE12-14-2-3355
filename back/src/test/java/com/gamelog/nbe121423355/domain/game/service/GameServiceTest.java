package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    @DisplayName("저장된 전체 게임을 목록 응답 DTO로 반환한다")
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
                .containsExactlyInAnyOrder(
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
    @DisplayName("저장된 게임이 없으면 빈 목록을 반환한다")
    void getGamesReturnsEmptyListWhenNoGamesExist() {
        // given
        assertThat(gameRepository.count()).isZero();

        // when
        List<GameListResponse> responses = gameService.getGames();

        // then
        assertThat(responses).isEmpty();
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
