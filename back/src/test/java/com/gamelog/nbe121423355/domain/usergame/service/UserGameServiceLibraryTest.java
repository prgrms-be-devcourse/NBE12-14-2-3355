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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserGameServiceLibraryTest {

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
    @DisplayName("라이브러리에 게임을 등록할 수 있다")
    void addGameToLibrary() {
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = org.mockito.Mockito.mock(Game.class);
        Platform platform = org.mockito.Mockito.mock(Platform.class);

        UserGameReqBody request = new UserGameReqBody(
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

        UserGameSaveResult result =
                userGameService.addOrUpdateGameToLibrary(
                        userId,
                        gameId,
                        request
                );

        assertThat(result.created()).isTrue();
        assertThat(result.userGame().getUser()).isEqualTo(user);
        assertThat(result.userGame().getGame()).isEqualTo(game);

        verify(userGameRepository).save(any(UserGame.class));
    }

    @Test
    @DisplayName("이미 등록된 게임이면 기존 UserGame을 수정한다")
    void updateExistingGame() {
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = org.mockito.Mockito.mock(Game.class);
        Platform platform = org.mockito.Mockito.mock(Platform.class);

        UserGame userGame = new UserGame(user, game);

        UserGameReqBody request = new UserGameReqBody(
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

        UserGameSaveResult result =
                userGameService.addOrUpdateGameToLibrary(
                        userId,
                        gameId,
                        request
                );

        assertThat(result.created()).isFalse();
        assertThat(result.userGame()).isEqualTo(userGame);
        assertThat(userGame.getPlayStatus()).isEqualTo(PlayStatus.PLAYED);
        assertThat(userGame.isPlaying()).isTrue();
        assertThat(userGame.isLiked()).isTrue();

        verify(userGameRepository, never()).save(any(UserGame.class));
    }

    @Test
    @DisplayName("등록된 게임의 플레이 기록을 수정할 수 있다")
    void updatePlayRecord() {
        Long userId = 1L;
        Long gameId = 1L;
        Long platformId = 1L;

        User user = new User("테스트", "test@test.com", "password");
        Game game = org.mockito.Mockito.mock(Game.class);
        Platform platform = org.mockito.Mockito.mock(Platform.class);
        UserGame userGame = new UserGame(user, game);

        UserGameReqBody request = new UserGameReqBody(
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

        UserGame result =
                userGameService.updatePlayRecord(
                        userId,
                        gameId,
                        request
                );

        assertThat(result).isSameAs(userGame);
        assertThat(result.getPlayStatus()).isEqualTo(PlayStatus.PLAYED);
        assertThat(result.isPlaying()).isTrue();
        assertThat(result.isLiked()).isTrue();
    }

    @Test
    @DisplayName("등록되지 않은 게임의 플레이 기록을 수정하면 예외가 발생한다")
    void updatePlayRecordWithoutLibrary() {
        Long userId = 1L;
        Long gameId = 1L;

        UserGameReqBody request = new UserGameReqBody(
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

        assertThatThrownBy(() ->
                userGameService.updatePlayRecord(userId, gameId, request)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");
    }

    @Test
    @DisplayName("완료일이 시작일보다 빠르면 저장할 수 없다")
    void rejectCompletedDateBeforeStartedDate() {
        LocalDate startedAt = LocalDate.now().minusDays(1);
        LocalDate completedAt = startedAt.minusDays(1);

        UserGameReqBody request = createRequest(
                PlayStatus.COMPLETED,
                startedAt,
                completedAt,
                null
        );

        assertThatThrownBy(() ->
                userGameService.addOrUpdateGameToLibrary(
                        1L,
                        1L,
                        request
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("완료일은 시작일보다 빠를 수 없습니다.");

        verifyNoInteractions(platformRepository, userGameRepository);
    }

    @Test
    @DisplayName("마지막 플레이 날짜가 미래면 저장할 수 없다")
    void rejectFutureLastPlayedDate() {
        LocalDateTime lastPlayedAt =
                LocalDate.now().plusDays(1).atStartOfDay();

        UserGameReqBody request =
                createRequest(
                        PlayStatus.PLAYED,
                        null,
                        null,
                        lastPlayedAt
                );

        assertThatThrownBy(() ->
                userGameService.addOrUpdateGameToLibrary(
                        1L,
                        1L,
                        request
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("마지막 플레이 날짜는 오늘보다 미래일 수 없습니다.");

        verifyNoInteractions(platformRepository, userGameRepository);
    }

    @Test
    @DisplayName("완료일이 미래면 저장할 수 없다")
    void rejectFutureCompletedDate() {
        UserGameReqBody request = createRequest(
                PlayStatus.COMPLETED,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                null
        );

        assertThatThrownBy(() ->
                userGameService.addOrUpdateGameToLibrary(
                        1L,
                        1L,
                        request
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("완료일은 오늘보다 미래일 수 없습니다.");

        verifyNoInteractions(platformRepository, userGameRepository);
    }

    @Test
    @DisplayName("시작일이 미래면 저장할 수 없다")
    void rejectFutureStartedDate() {
        UserGameReqBody request = createRequest(
                PlayStatus.PLAYED,
                LocalDate.now().plusDays(1),
                null,
                null
        );

        assertThatThrownBy(() ->
                userGameService.addOrUpdateGameToLibrary(
                        1L,
                        1L,
                        request
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("시작일은 오늘보다 미래일 수 없습니다.");

        verifyNoInteractions(platformRepository, userGameRepository);
    }

    @Test
    @DisplayName("마지막 플레이 날짜가 시작일보다 빠르면 저장할 수 없다")
    void rejectLastPlayedDateBeforeStartedDate() {
        LocalDate startedAt = LocalDate.now().minusDays(1);

        UserGameReqBody request = createRequest(
                PlayStatus.PLAYED,
                startedAt,
                null,
                startedAt.minusDays(1).atStartOfDay()
        );

        assertThatThrownBy(() ->
                userGameService.addOrUpdateGameToLibrary(
                        1L,
                        1L,
                        request
                )
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("마지막 플레이 날짜는 시작일보다 빠를 수 없습니다.");

        verifyNoInteractions(platformRepository, userGameRepository);
    }

    @Test
    @DisplayName("플레이 상태를 변경할 수 있다")
    void changePlayedStatus() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        PlayStatus result =
                userGameService.changePlayed(
                        1L,
                        1L,
                        PlayStatus.DROPPED
                );

        assertThat(result).isEqualTo(PlayStatus.DROPPED);
        assertThat(userGame.getPlayStatus())
                .isEqualTo(PlayStatus.DROPPED);
    }

    @Test
    @DisplayName("UserGame이 없으면 생성하고 플레이 상태를 변경한다")
    void createUserGameAndChangePlayedStatus() {
        User user = new User("테스트", "test@test.com", "password");
        Game game = org.mockito.Mockito.mock(Game.class);

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(1L))
                .willReturn(Optional.of(user));
        given(gameRepository.findById(1L))
                .willReturn(Optional.of(game));
        given(userGameRepository.save(any(UserGame.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        PlayStatus result =
                userGameService.changePlayed(
                        1L,
                        1L,
                        PlayStatus.COMPLETED
                );

        assertThat(result).isEqualTo(PlayStatus.COMPLETED);

        ArgumentCaptor<UserGame> captor =
                ArgumentCaptor.forClass(UserGame.class);

        verify(userGameRepository).save(captor.capture());

        UserGame saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getGame()).isEqualTo(game);
        assertThat(saved.getPlayStatus())
                .isEqualTo(PlayStatus.COMPLETED);
    }

    @Test
    @DisplayName("playing을 변경할 수 있다")
    void changePlaying() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        boolean result =
                userGameService.changePlaying(1L, 1L, true);

        assertThat(result).isTrue();
        assertThat(userGame.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("UserGame이 없을 때 playing을 false로 변경하면 예외가 발생한다")
    void changePlayingFalseWithoutUserGame() {
        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changePlaying(1L, 1L, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
        verify(userRepository, never()).findById(anyLong());
        verify(gameRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("wishlist를 변경할 수 있다")
    void changeWishlist() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        boolean result =
                userGameService.changeWishlist(1L, 1L, true);

        assertThat(result).isTrue();
        assertThat(userGame.isWishlist()).isTrue();
    }

    @Test
    @DisplayName("backlog를 변경할 수 있다")
    void changeBacklog() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        boolean result =
                userGameService.changeBacklog(1L, 1L, true);

        assertThat(result).isTrue();
        assertThat(userGame.isBacklog()).isTrue();
    }

    @Test
    @DisplayName("liked를 변경할 수 있다")
    void changeLiked() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        boolean result =
                userGameService.changeLiked(1L, 1L, true);

        assertThat(result).isTrue();
        assertThat(userGame.isLiked()).isTrue();
    }

    @Test
    @DisplayName("같은 playing 상태를 반복해서 변경해도 상태가 유지된다")
    void repeatPlayingChange() {
        UserGame userGame = createUserGame();

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        assertThat(
                userGameService.changePlaying(1L, 1L, true)
        ).isTrue();

        assertThat(
                userGameService.changePlaying(1L, 1L, true)
        ).isTrue();

        assertThat(userGame.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("플레이 상태가 null이면 기존 플레이 상태를 초기화한다")
    void clearPlayStatus() {
        UserGame userGame = createUserGame();
        userGame.changePlayStatus(PlayStatus.PLAYED);

        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.of(userGame));

        PlayStatus result =
                userGameService.changePlayed(1L, 1L, null);

        assertThat(result).isNull();
        assertThat(userGame.getPlayStatus()).isNull();
    }

    @Test
    @DisplayName("UserGame이 없을 때 wishlist를 false로 변경하면 예외가 발생한다")
    void changeWishlistFalseWithoutUserGame() {
        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeWishlist(1L, 1L, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이 없을 때 backlog를 false로 변경하면 예외가 발생한다")
    void changeBacklogFalseWithoutUserGame() {
        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeBacklog(1L, 1L, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이 없을 때 liked를 false로 변경하면 예외가 발생한다")
    void changeLikedFalseWithoutUserGame() {
        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changeLiked(1L, 1L, false)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("UserGame이 없을 때 playStatus를 null로 변경하면 예외가 발생한다")
    void clearPlayStatusWithoutUserGame() {
        given(userGameRepository.findByUser_IdAndGame_Id(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGameService.changePlayed(1L, 1L, null)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessage("라이브러리에 등록되지 않은 게임입니다.");

        verify(userGameRepository, never())
                .save(any(UserGame.class));
    }

    @Test
    @DisplayName("라이브러리 게임 목록을 페이지네이션으로 조회한다")
    void getUserGameList() {
        User user = new User("테스트", "test@test.com", "password");

        Game game1 = org.mockito.Mockito.mock(Game.class);
        Game game2 = org.mockito.Mockito.mock(Game.class);

        given(game1.getId()).willReturn(1L);
        given(game1.getTitle()).willReturn("게임1");
        given(game1.getCoverImageUrl()).willReturn("image1.jpg");

        given(game2.getId()).willReturn(2L);
        given(game2.getTitle()).willReturn("게임2");
        given(game2.getCoverImageUrl()).willReturn("image2.jpg");

        Pageable pageable = PageRequest.of(0, 2);

        Page<UserGame> page = new PageImpl<>(
                List.of(
                        new UserGame(user, game1),
                        new UserGame(user, game2)
                ),
                pageable,
                2
        );

        given(userGameRepository.findAllByUser_IdAndInLibraryTrue(
                1L,
                pageable
        )).willReturn(page);

        Page<UserGameListResponse> result =
                userGameService.getUserGameList(1L, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).gameId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).gameId()).isEqualTo(2L);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(userGameRepository)
                .findAllByUser_IdAndInLibraryTrue(1L, pageable);
    }

    @Test
    @DisplayName("라이브러리에 게임이 없으면 빈 페이지를 반환한다")
    void getEmptyUserGameList() {
        Pageable pageable = PageRequest.of(0, 2);

        given(userGameRepository.findAllByUser_IdAndInLibraryTrue(
                1L,
                pageable
        )).willReturn(
                new PageImpl<>(List.of(), pageable, 0)
        );

        Page<UserGameListResponse> result =
                userGameService.getUserGameList(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    private UserGame createUserGame() {
        User user = new User("테스트", "test@test.com", "password");
        Game game = org.mockito.Mockito.mock(Game.class);

        return new UserGame(user, game);
    }

    private UserGameReqBody createRequest(
            PlayStatus playStatus,
            LocalDate startedAt,
            LocalDate completedAt,
            LocalDateTime lastPlayedAt
    ) {
        return new UserGameReqBody(
                playStatus,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                startedAt,
                completedAt,
                lastPlayedAt
        );
    }
}