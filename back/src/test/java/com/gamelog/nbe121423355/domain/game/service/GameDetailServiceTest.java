package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.global.exception.ServiceException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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

        Game game = gameRepository.save(
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
        GameDetailResponse response =
                gameDetailService.getGameDetail(gameId);

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
    }

    @DisplayName("존재하지 않는 게임 ID를 조회하면 예외가 발생한다")
    @Test
    void getGameDetailThrowsExceptionWhenGameDoesNotExist() {
        assertThatThrownBy(
                () -> gameDetailService.getGameDetail(Long.MAX_VALUE)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("존재하지 않는 게임입니다.");
    }
    
}
