package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGameServiceTest {

    @InjectMocks
    private UserGameService userGameService;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Test
    @DisplayName("라이브러리에_게임을_등록")
    void t1() {
        // given
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");

        Game game = mock(Game.class);
        Platform platform = mock(Platform.class);

        UserGameReqBody reqBody = new UserGameReqBody(
                PlayStatus.PLAYED,
                false,
                false,
                false,
                true,
                platformId,
                new BigDecimal("42.5"),
                new BigDecimal("35.0"),
                new BigDecimal("80.0"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 20),
                LocalDateTime.of(2026, 8, 20, 21, 30)
        );

        given(platformRepository.findById(platformId))
                .willReturn(Optional.of(platform));

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(gameRepository.findById(gameId))
                .willReturn(Optional.of(game));

        given(userGameRepository.save(any(UserGame.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserGameSaveResult result =
                userGameService.addOrUpdateGameToLibrary(
                        userId,
                        gameId,
                        reqBody
                );

        // then
        assertThat(result.created()).isTrue();
        assertThat(result.userGame()).isNotNull();
        assertThat(result.userGame().getUser()).isEqualTo(user);
        assertThat(result.userGame().getGame()).isEqualTo(game);

        verify(userGameRepository).save(any(UserGame.class));
    }

    @Test
    @DisplayName("이미_등록된_게임이면_수정")
    void t2() {
        // given
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        Platform platform = mock(Platform.class);

        UserGame userGame = new UserGame(user, game);

        UserGameReqBody reqBody = new UserGameReqBody(
                PlayStatus.PLAYED,
                true,
                false,
                false,
                true,
                platformId,
                new BigDecimal("42.5"),
                new BigDecimal("35.0"),
                new BigDecimal("80.0"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 20),
                LocalDateTime.of(2026, 8, 20, 21, 30)
        );

        given(platformRepository.findById(platformId))
                .willReturn(Optional.of(platform));

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        UserGameSaveResult result =
                userGameService.addOrUpdateGameToLibrary(
                        userId,
                        gameId,
                        reqBody
                );

        // then
        assertThat(result.created()).isFalse();
        assertThat(result.userGame()).isEqualTo(userGame);

        assertThat(userGame.getPlayStatus())
                .isEqualTo(PlayStatus.PLAYED);
        assertThat(userGame.isPlaying()).isTrue();
        assertThat(userGame.isLiked()).isTrue();

        verify(userGameRepository, never()).save(any(UserGame.class));
    }

    @Test
    @DisplayName("등록된_게임의_플레이_기록을_수정")
    void t3() {
        // given
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        Platform platform = mock(Platform.class);

        UserGame userGame = new UserGame(user, game);

        UserGameReqBody reqBody = new UserGameReqBody(
                PlayStatus.PLAYED,
                true,
                false,
                false,
                true,
                platformId,
                new BigDecimal("50.0"),
                new BigDecimal("40.0"),
                new BigDecimal("80.0"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 20),
                LocalDateTime.of(2026, 8, 20, 21, 30)
        );

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        given(platformRepository.findById(platformId))
                .willReturn(Optional.of(platform));

        // when
        UserGame result =
                userGameService.updatePlayRecord(
                        userId,
                        gameId,
                        reqBody
                );

        // then
        assertThat(result).isEqualTo(userGame);
        assertThat(result.getPlayStatus())
                .isEqualTo(PlayStatus.PLAYED);
        assertThat(result.isPlaying()).isTrue();
        assertThat(result.isLiked()).isTrue();
    }

    @Test
    @DisplayName("등록되지_않은_게임을_수정하면_예외가_발생")
    void t4() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        UserGameReqBody reqBody = new UserGameReqBody(
                PlayStatus.PLAYED,
                false,
                false,
                false,
                false,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null
        );

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userGameService.updatePlayRecord(
                        userId,
                        gameId,
                        reqBody
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");
    }

}
