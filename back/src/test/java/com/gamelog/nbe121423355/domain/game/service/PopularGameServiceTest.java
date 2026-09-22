package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.PopularGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.GameGenre;
import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GameGenreRepository;
import com.gamelog.nbe121423355.domain.game.repository.GameStatisticsRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.PopularGameProjection;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularGameServiceTest {

    @Mock
    private GameStatisticsRepository gameStatisticsRepository;

    @Mock
    private GameGenreRepository gameGenreRepository;

    @InjectMocks
    private PopularGameService popularGameService;

    @Test
    @DisplayName("인기 게임 기본 정보와 장르를 함께 반환한다")
    void getPopularGamesReturnsGamesWithGenres() {
        // given: 인기 게임 한 개와 해당 게임의 장르 두 개를 준비
        PopularGameProjection popularGame =
                org.mockito.Mockito.mock(PopularGameProjection.class);
        Game game = org.mockito.Mockito.mock(Game.class);
        Genre action = org.mockito.Mockito.mock(Genre.class);
        Genre rpg = org.mockito.Mockito.mock(Genre.class);
        GameGenre actionGenre = org.mockito.Mockito.mock(GameGenre.class);
        GameGenre rpgGenre = org.mockito.Mockito.mock(GameGenre.class);

        when(popularGame.getGameId()).thenReturn(1L);
        when(popularGame.getTitle()).thenReturn("인기 게임");
        when(popularGame.getCoverImageUrl()).thenReturn("https://image.test/game.jpg");
        when(popularGame.getLikeCount()).thenReturn(10L);

        when(game.getId()).thenReturn(1L);
        when(action.getId()).thenReturn(11L);
        when(action.getName()).thenReturn("Action");
        when(rpg.getId()).thenReturn(12L);
        when(rpg.getName()).thenReturn("RPG");
        when(actionGenre.getGame()).thenReturn(game);
        when(actionGenre.getGenre()).thenReturn(action);
        when(rpgGenre.getGame()).thenReturn(game);
        when(rpgGenre.getGenre()).thenReturn(rpg);

        when(gameStatisticsRepository.findPopularGames(PageRequest.of(0, 5)))
                .thenReturn(List.of(popularGame));
        when(gameGenreRepository.findAllWithGenreByGameIdIn(List.of(1L)))
                .thenReturn(List.of(actionGenre, rpgGenre));

        // when: 기본 크기로 인기 게임을 조회
        List<PopularGameResponse> result =
                popularGameService.getPopularGames(5);

        // then: 게임 정보와 모든 장르가 하나의 응답으로 조립
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().gameId()).isEqualTo(1L);
        assertThat(result.getFirst().title()).isEqualTo("인기 게임");
        assertThat(result.getFirst().coverImageUrl())
                .isEqualTo("https://image.test/game.jpg");
        assertThat(result.getFirst().likeCount()).isEqualTo(10L);
        assertThat(result.getFirst().genres())
                .extracting(PopularGameResponse.GenreResponse::name)
                .containsExactly("Action", "RPG");
    }

    @Test
    @DisplayName("인기 게임이 없으면 장르를 조회하지 않고 빈 목록을 반환한다")
    void getPopularGamesReturnsEmptyListWithoutGenreQuery() {
        // given: 인기 게임 조회 결과가 비어 있도록 준비
        when(gameStatisticsRepository.findPopularGames(PageRequest.of(0, 5)))
                .thenReturn(List.of());

        // when: 인기 게임을 조회
        List<PopularGameResponse> result =
                popularGameService.getPopularGames(5);

        // then: 빈 목록을 반환하고 장르 Repository는 호출하지 않음
        assertThat(result).isEmpty();
        verify(gameGenreRepository, never())
                .findAllWithGenreByGameIdIn(org.mockito.ArgumentMatchers.anyList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("조회 개수가 허용 범위를 벗어나면 예외가 발생한다")
    void getPopularGamesRejectsInvalidSize(int size) {
        // given: 허용 범위를 벗어난 조회 개수를 준비

        // when: 잘못된 조회 개수로 인기 게임 조회를 요청
        // then: 400 예외가 발생하고 Repository는 호출하지 않음
        assertThatThrownBy(() -> popularGameService.getPopularGames(size))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("400-9"));

        verifyNoInteractions(
                gameStatisticsRepository,
                gameGenreRepository
        );
    }
}
