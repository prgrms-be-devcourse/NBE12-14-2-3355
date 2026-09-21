package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
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
import java.util.ArrayList;
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

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserFavoriteGameRepository userFavoriteGameRepository;

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

    @Test
    @DisplayName("프로필 탭 조회 - 플레이 요약과 취향 분석을 정상적으로 반환한다")
    void t21() {
        // given
        Long userId = 1L;

        UserGame game1 = mock(UserGame.class);
        UserGame game2 = mock(UserGame.class);
        UserGame game3 = mock(UserGame.class);

        when(game1.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(40));
        when(game2.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(20));
        when(game3.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(10));

        when(game1.getPlayStatus())
                .thenReturn(PlayStatus.COMPLETED);
        when(game2.getPlayStatus())
                .thenReturn(PlayStatus.PLAYED);
        when(game3.getPlayStatus())
                .thenReturn(PlayStatus.COMPLETED);

        List<UserGame> playedGames =
                List.of(game1, game2, game3);

        List<BigDecimal> ratings = List.of(
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(2.0)
        );

        List<UserGameScatterDto> scatterData = List.of(
                mock(UserGameScatterDto.class),
                mock(UserGameScatterDto.class),
                mock(UserGameScatterDto.class)
        );

        List<UserGameGenreDTO> genreData = List.of(
                new UserGameGenreDTO(
                        1L,
                        "RPG",
                        2L
                ),
                new UserGameGenreDTO(
                        2L,
                        "Action",
                        1L
                )
        );

        when(userGameRepository.findPlayedGames(userId))
                .thenReturn(playedGames);

        when(reviewRepository.findPlayedGameRatings(userId))
                .thenReturn(ratings);

        when(reviewRepository.findAverageRating(userId))
                .thenReturn(BigDecimal.valueOf(3.3));

        when(userGameRepository.findPlayedGameScatterData(userId))
                .thenReturn(scatterData);

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(genreData);

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        assertThat(response).isNotNull();

        // 플레이 요약
        assertThat(response.stats().playedGameCount())
                .isEqualTo(3);

        assertThat(response.stats().averageRating())
                .isEqualByComparingTo("3.3");

        assertThat(response.stats().totalPlayTime())
                .isEqualByComparingTo("70");

        // 산점도
        assertThat(response.scatterData())
                .hasSize(3);

        // 취향 분석
        assertThat(response.tasteResponse())
                .isNotNull();

        // 장르 분포
        assertThat(response.genreDistribution())
                .hasSize(2);
    }

    @Test
    @DisplayName("장시간 플레이 게임이 40% 이상이면 장시간 플레이와 짧은 게임을 골고루 즐긴다고 판단한다")
    void t22() {
        // given
        Long userId = 1L;

        UserGame game1 = mock(UserGame.class);
        UserGame game2 = mock(UserGame.class);
        UserGame game3 = mock(UserGame.class);

        when(game1.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(30));
        when(game2.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(50));
        when(game3.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(10));

        when(userGameRepository.findPlayedGames(userId))
                .thenReturn(List.of(game1, game2, game3));

        when(reviewRepository.findPlayedGameRatings(userId))
                .thenReturn(List.of());

        when(reviewRepository.findAverageRating(userId))
                .thenReturn(BigDecimal.ZERO);

        when(userGameRepository.findPlayedGameScatterData(userId))
                .thenReturn(List.of());

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of());

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        TasteMetricDto longPlay =
                response.tasteResponse().longPlay();

        assertThat(longPlay.ratio())
                .isEqualByComparingTo("0.6667");

        assertThat(longPlay.message())
                .isEqualTo("장시간 플레이와 짧은 게임을 골고루 즐겨요.");
    }

    @Test
    @DisplayName("장시간 플레이 비율이 70% 이상이면 장시간 플레이 성향으로 판단한다")
    void t23() {
        // given
        Long userId = 1L;

        UserGame game1 = mock(UserGame.class);
        UserGame game2 = mock(UserGame.class);
        UserGame game3 = mock(UserGame.class);

        when(game1.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(30));
        when(game2.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(40));
        when(game3.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(50));

        mockProfileRepositories(
                userId,
                List.of(game1, game2, game3)
        );

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        TasteMetricDto result =
                response.tasteResponse().longPlay();

        assertThat(result.ratio())
                .isEqualByComparingTo("1");

        assertThat(result.message())
                .isEqualTo("장시간 플레이하는 게임이 많아요.");
    }

    @Test
    @DisplayName("플레이 시간 기록이 3개 미만이면 장시간 플레이 성향을 분석하지 않는다")
    void t24() {
        // given
        Long userId = 1L;

        UserGame game1 = mock(UserGame.class);
        UserGame game2 = mock(UserGame.class);

        when(game1.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(30));
        when(game2.getPlayTimeHours())
                .thenReturn(BigDecimal.valueOf(20));

        mockProfileRepositories(
                userId,
                List.of(game1, game2)
        );

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        TasteMetricDto result =
                response.tasteResponse().longPlay();

        assertThat(result.ratio())
                .isNull();

        assertThat(result.message())
                .isEqualTo("아직 플레이 기록이 부족해요.");
    }

    @Test
    @DisplayName("2.5 이상 4.0 미만 평점이 가장 많으면 중간 평점 성향으로 판단한다")
    void t26() {
        // given
        Long userId = 1L;

        List<BigDecimal> ratings = List.of(
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(2.0)
        );

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(reviewRepository.findPlayedGameRatings(userId))
                .thenReturn(ratings);

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        TasteMetricDto result =
                response.tasteResponse().rating();

        assertThat(result.ratio())
                .isEqualByComparingTo("0.5");

        assertThat(result.message())
                .isEqualTo("게임을 비교적 후하게 평가하는 편이에요.");
    }

    @Test
    @DisplayName("높은 평점과 중간 평점이 동률이면 중간 평점 성향을 선택한다")
    void t27() {
        // given
        Long userId = 1L;

        List<BigDecimal> ratings = List.of(
                BigDecimal.valueOf(4.0),
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(3.5)
        );

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(reviewRepository.findPlayedGameRatings(userId))
                .thenReturn(ratings);

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        assertThat(response.tasteResponse().rating().message())
                .isEqualTo("게임을 비교적 후하게 평가하는 편이에요.");
    }

    @Test
    @DisplayName("완료 비율이 40% 이상 70% 미만이면 절반 정도를 완료했다고 판단한다")
    void t28() {
        // given
        Long userId = 1L;

        UserGame game1 = mock(UserGame.class);
        UserGame game2 = mock(UserGame.class);
        UserGame game3 = mock(UserGame.class);

        when(game1.getPlayStatus())
                .thenReturn(PlayStatus.COMPLETED);

        when(game2.getPlayStatus())
                .thenReturn(PlayStatus.COMPLETED);

        when(game3.getPlayStatus())
                .thenReturn(PlayStatus.PLAYED);

        mockProfileRepositories(
                userId,
                List.of(game1, game2, game3)
        );

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        TasteMetricDto result =
                response.tasteResponse().completion();

        assertThat(result.ratio())
                .isEqualByComparingTo("0.6667");

        assertThat(result.message())
                .isEqualTo("플레이한 게임 중 절반 정도를 완료했어요.");
    }

    @Test
    @DisplayName("장르별 게임 수를 기준으로 장르 비율을 계산한다")
    void t29() {
        // given
        Long userId = 1L;

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of(
                        new UserGameGenreDTO(1L, "RPG", 2L),
                        new UserGameGenreDTO(2L, "Action", 1L)
                ));

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        List<UserGameGenreDistributionResponse> result =
                response.genreDistribution();

        assertThat(result).hasSize(2);

        assertThat(result.get(0).genreName())
                .isEqualTo("RPG");

        assertThat(result.get(0).ratio())
                .isEqualByComparingTo("0.6667");

        assertThat(result.get(1).genreName())
                .isEqualTo("Action");

        assertThat(result.get(1).ratio())
                .isEqualByComparingTo("0.3333");
    }

    @Test
    @DisplayName("장르 데이터가 없으면 빈 목록을 반환한다")
    void t30() {
        // given
        Long userId = 1L;

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of());

        // when
        UserProfileResponse response =
                userGameService.profileTab(userId);

        // then
        assertThat(response.genreDistribution())
                .isEmpty();
    }

    private void mockProfileRepositories(
            Long userId,
            List<UserGame> playedGames
    ) {
        when(userGameRepository.findPlayedGames(userId))
                .thenReturn(playedGames);

        when(reviewRepository.findPlayedGameRatings(userId))
                .thenReturn(List.of());

        when(reviewRepository.findAverageRating(userId))
                .thenReturn(BigDecimal.ZERO);

        when(userGameRepository.findPlayedGameScatterData(userId))
                .thenReturn(List.of());

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of());
    }

    private List<UserGame> createPlayedGames(int count) {
        List<UserGame> games = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            UserGame game = mock(UserGame.class);

            when(game.getPlayTimeHours())
                    .thenReturn(BigDecimal.valueOf(10));

            when(game.getPlayStatus())
                    .thenReturn(PlayStatus.PLAYED);

            games.add(game);
        }

        return games;
    }

    @Test
    @DisplayName("인생게임을 등록할 수 있다")
    void t31() {
        // given
        Long userId = 1L;

        User user = mock(User.class);

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(game1.getId()).thenReturn(10L);
        when(game2.getId()).thenReturn(20L);

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        when(userGame1.getGame()).thenReturn(game1);
        when(userGame2.getGame()).thenReturn(game2);

        when(userGameRepository
                .findAllByUserIdAndGameIdIn(userId, List.of(10L, 20L)))
                .thenReturn(List.of(userGame1, userGame2));

        // when
        List<UserFavoriteGameResponse> result =
                userGameService.updateFavoriteGames(
                        userId,
                        List.of(10L, 20L)
                );

        // then
        verify(userFavoriteGameRepository)
                .deleteAllByUserId(userId);

        verify(userFavoriteGameRepository)
                .saveAll(anyList());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("인생게임은 최대 5개까지 등록할 수 있다")
    void t32() {
        // given
        Long userId = 1L;

        List<Long> gameIds = List.of(
                1L, 2L, 3L, 4L, 5L, 6L
        );

        // when & then
        assertThatThrownBy(() ->
                userGameService.updateFavoriteGames(userId, gameIds)
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인생게임에는 같은 게임을 중복 등록할 수 없다")
    void t33() {
        // given
        Long userId = 1L;

        List<Long> gameIds = List.of(
                10L, 20L, 10L
        );

        // when & then
        assertThatThrownBy(() ->
                userGameService.updateFavoriteGames(userId, gameIds)
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최근 플레이한 게임을 최대 5개 조회한다")
    void t34() {
        // given
        Long userId = 1L;

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        when(userGame1.getGame()).thenReturn(game1);
        when(userGame2.getGame()).thenReturn(game2);

        when(game1.getId()).thenReturn(1L);
        when(game2.getId()).thenReturn(2L);

        when(game1.getTitle()).thenReturn("Game 1");
        when(game2.getTitle()).thenReturn("Game 2");

        when(game1.getCoverImageUrl()).thenReturn("cover1");
        when(game2.getCoverImageUrl()).thenReturn("cover2");

        when(userGameRepository.findRecentPlayedGames(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(List.of(userGame1, userGame2));

        // when
        List<UserGameListResponse> result =
                userGameService.getRecentPlayedGames(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).gameId()).isEqualTo(1L);
        assertThat(result.get(1).gameId()).isEqualTo(2L);

        verify(userGameRepository)
                .findRecentPlayedGames(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("최근 플레이 게임은 최대 5개를 요청한다")
    void t35() {
        // given
        Long userId = 1L;

        when(userGameRepository.findRecentPlayedGames(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(List.of());

        // when
        userGameService.getRecentPlayedGames(userId);

        // then
        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(userGameRepository)
                .findRecentPlayedGames(eq(userId), captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("최근 작성한 리뷰를 최대 3개 조회한다")
    void t36() {
        // given
        Long userId = 1L;

        Review review1 = mock(Review.class);
        Review review2 = mock(Review.class);

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        when(review1.getId()).thenReturn(1L);
        when(review2.getId()).thenReturn(2L);

        when(review1.getUserGame()).thenReturn(userGame1);
        when(review2.getUserGame()).thenReturn(userGame2);

        when(userGame1.getGame()).thenReturn(game1);
        when(userGame2.getGame()).thenReturn(game2);

        when(game1.getId()).thenReturn(10L);
        when(game2.getId()).thenReturn(20L);

        when(game1.getTitle()).thenReturn("Game 1");
        when(game2.getTitle()).thenReturn("Game 2");

        when(game1.getCoverImageUrl()).thenReturn("cover1");
        when(game2.getCoverImageUrl()).thenReturn("cover2");

        when(reviewRepository.findRecentReviews(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(List.of(review1, review2));

        // when
        List<RecentReviewResponse> result =
                userGameService.getRecentReviews(userId);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).reviewId())
                .isEqualTo(1L);

        assertThat(result.get(0).gameId())
                .isEqualTo(10L);

        assertThat(result.get(1).reviewId())
                .isEqualTo(2L);

        assertThat(result.get(1).gameId())
                .isEqualTo(20L);
    }

    @Test
    @DisplayName("최근 리뷰는 최대 3개를 요청한다")
    void t37() {
        // given
        Long userId = 1L;

        when(reviewRepository.findRecentReviews(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(List.of());

        // when
        userGameService.getRecentReviews(userId);

        // then
        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(reviewRepository)
                .findRecentReviews(eq(userId), captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(3);
    }


}
