package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameListResponse;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    @DisplayName("플레이_상태를_변경")
    void t5() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);

        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        PlayStatus result =
                userGameService.changePlayed(
                        userId,
                        gameId,
                        PlayStatus.DROPPED
                );

        // then
        assertThat(result).isEqualTo(PlayStatus.DROPPED);
        assertThat(userGame.getPlayStatus())
                .isEqualTo(PlayStatus.DROPPED);

        verify(userGameRepository, never()).save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이_없으면_생성하고_플레이_상태를_변경")
    void t6() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(gameRepository.findById(gameId))
                .willReturn(Optional.of(game));

        given(userGameRepository.save(any(UserGame.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlayStatus result =
                userGameService.changePlayed(
                        userId,
                        gameId,
                        PlayStatus.COMPLETED
                );

        // then
        assertThat(result).isEqualTo(PlayStatus.COMPLETED);

        ArgumentCaptor<UserGame> captor =
                ArgumentCaptor.forClass(UserGame.class);

        verify(userGameRepository).save(captor.capture());

        UserGame savedUserGame = captor.getValue();

        assertThat(savedUserGame.getUser()).isEqualTo(user);
        assertThat(savedUserGame.getGame()).isEqualTo(game);
        assertThat(savedUserGame.getPlayStatus())
                .isEqualTo(PlayStatus.COMPLETED);
    }

    @Test
    @DisplayName("playing을_true로_변경")
    void t7() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        boolean result =
                userGameService.changePlaying(userId, gameId, true);

        // then
        assertThat(result).isTrue();
        assertThat(userGame.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("UserGame이_없을_때_playing_false면_예외가_발생한다")
    void t8() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changePlaying(userId, gameId, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));

        verify(userRepository, never())
                .findById(anyLong());

        verify(gameRepository, never())
                .findById(anyLong());
    }

    @Test
    @DisplayName("wishlist를_변경")
    void t9() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        boolean result =
                userGameService.changeWishlist(userId, gameId, true);

        // then
        assertThat(result).isTrue();
        assertThat(userGame.isWishlist()).isTrue();
    }

    @Test
    @DisplayName("backlog를_변경")
    void t10() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        boolean result =
                userGameService.changeBacklog(userId, gameId, true);

        // then
        assertThat(result).isTrue();
        assertThat(userGame.isBacklog()).isTrue();
    }

    @Test
    @DisplayName("liked를_변경")
    void t11() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        boolean result =
                userGameService.changeLiked(userId, gameId, true);

        // then
        assertThat(result).isTrue();
        assertThat(userGame.isLiked()).isTrue();
    }

    @Test
    @DisplayName("같은_playing_상태를_반복해서_변경해도_상태가_유지된다")
    void t12() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);
        UserGame userGame = new UserGame(user, game);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        boolean firstResult =
                userGameService.changePlaying(userId, gameId, true);

        boolean secondResult =
                userGameService.changePlaying(userId, gameId, true);

        // then
        assertThat(firstResult).isTrue();
        assertThat(secondResult).isTrue();
        assertThat(userGame.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("플레이_상태가_null이면_플레이_상태를_초기화")
    void t13() {
        // given
        Long userId = 1L;
        Long gameId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = mock(Game.class);

        UserGame userGame = new UserGame(user, game);
        userGame.changePlayStatus(PlayStatus.PLAYED);

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.of(userGame));

        // when
        PlayStatus result =
                userGameService.changePlayed(userId, gameId, null);

        // then
        assertThat(result).isNull();
        assertThat(userGame.getPlayStatus()).isNull();
    }

    @Test
    @DisplayName("UserGame이_없을_때_playing_false면_예외가_발생한다")
    void t14() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changePlaying(userId, gameId, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이_없을_때_wishlist_false면_예외가_발생한다")
    void t15() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeWishlist(userId, gameId, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이_없을_때_backlog_false면_예외가_발생한다")
    void t16() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeBacklog(userId, gameId, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이_없을_때_liked_false면_예외가_발생한다")
    void t17() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeLiked(userId, gameId, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이_없을_때_playStatus_null이면_예외가_발생한다")
    void t18() {
        Long userId = 1L;
        Long gameId = 1L;

        given(userGameRepository.findByUser_IdAndGame_Id(userId, gameId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changePlayed(userId, gameId, null)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("라이브러리_게임_목록을_페이지네이션으로_조회")
    void t19() {
        // given
        Long userId = 1L;

        User user = new User(
                "테스트",
                "test@test.com",
                "password"
        );

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        given(game1.getId()).willReturn(1L);
        given(game1.getTitle()).willReturn("게임1");
        given(game1.getCoverImageUrl()).willReturn("image1.jpg");

        given(game2.getId()).willReturn(2L);
        given(game2.getTitle()).willReturn("게임2");
        given(game2.getCoverImageUrl()).willReturn("image2.jpg");

        UserGame userGame1 = new UserGame(user, game1);
        UserGame userGame2 = new UserGame(user, game2);

        Pageable pageable = PageRequest.of(0, 2);

        Page<UserGame> userGamePage = new PageImpl<>(
                List.of(userGame1, userGame2),
                pageable,
                2
        );

        given(userGameRepository.findAllByUser_IdAndInLibraryTrue(
                userId,
                pageable
        )).willReturn(userGamePage);

        // when
        Page<UserGameListResponse> result =
                userGameService.getUserGameList(userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent().get(0).gameId())
                .isEqualTo(1L);
        assertThat(result.getContent().get(0).title())
                .isEqualTo("게임1");
        assertThat(result.getContent().get(0).coverImageUrl())
                .isEqualTo("image1.jpg");

        assertThat(result.getContent().get(1).gameId())
                .isEqualTo(2L);
        assertThat(result.getContent().get(1).title())
                .isEqualTo("게임2");
        assertThat(result.getContent().get(1).coverImageUrl())
                .isEqualTo("image2.jpg");

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);

        verify(userGameRepository)
                .findAllByUser_IdAndInLibraryTrue(userId, pageable);
    }

    @Test
    @DisplayName("라이브러리에_게임이_없으면_빈_페이지를_반환")
    void t20() {
        // given
        Long userId = 1L;

        Pageable pageable = PageRequest.of(0, 2);

        Page<UserGame> emptyPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        given(userGameRepository.findAllByUser_IdAndInLibraryTrue(
                userId,
                pageable
        )).willReturn(emptyPage);

        // when
        Page<UserGameListResponse> result =
                userGameService.getUserGameList(userId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();

        verify(userGameRepository)
                .findAllByUser_IdAndInLibraryTrue(userId, pageable);
    }
}
