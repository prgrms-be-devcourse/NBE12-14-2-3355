package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserFavoriteGameResponse;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGameServiceFavoriteTest {

    @InjectMocks
    private UserGameService userGameService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserFavoriteGameRepository userFavoriteGameRepository;

    @Test
    @DisplayName("인생게임을 등록할 수 있다")
    void updateFavoriteGames() {
        Long userId = 1L;

        User user = mock(User.class);
        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);

        UserGame userGame1 = mock(UserGame.class);
        UserGame userGame2 = mock(UserGame.class);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(game1.getId()).willReturn(10L);
        given(game2.getId()).willReturn(20L);

        given(userGame1.getGame()).willReturn(game1);
        given(userGame2.getGame()).willReturn(game2);

        given(userGameRepository.findAllByUserIdAndGameIdIn(
                userId,
                List.of(10L, 20L)
        )).willReturn(List.of(userGame1, userGame2));

        List<UserFavoriteGameResponse> result =
                userGameService.updateFavoriteGames(
                        userId,
                        List.of(10L, 20L)
                );

        verify(userFavoriteGameRepository)
                .deleteAllByUserId(userId);

        verify(userFavoriteGameRepository)
                .saveAll(anyList());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("인생게임은 최대 5개까지 등록할 수 있다")
    void rejectMoreThanFiveFavoriteGames() {
        List<Long> gameIds =
                List.of(1L, 2L, 3L, 4L, 5L, 6L);

        assertThatThrownBy(() ->
                userGameService.updateFavoriteGames(1L, gameIds)
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인생게임에는 같은 게임을 중복 등록할 수 없다")
    void rejectDuplicateFavoriteGames() {
        List<Long> gameIds =
                List.of(10L, 20L, 10L);

        assertThatThrownBy(() ->
                userGameService.updateFavoriteGames(1L, gameIds)
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인생게임 목록에 빈 리스트를 전달하면 기존 인생게임을 모두 삭제한다")
    void clearFavoriteGames() {
        Long userId = 1L;

        User user = mock(User.class);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        List<UserFavoriteGameResponse> result =
                userGameService.updateFavoriteGames(
                        userId,
                        List.of()
                );

        verify(userFavoriteGameRepository)
                .deleteAllByUserId(userId);

        verify(userFavoriteGameRepository)
                .saveAll(anyList());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("라이브러리에 등록하지 않은 게임은 인생게임으로 등록할 수 없다")
    void rejectGameNotInLibrary() {
        Long userId = 1L;

        User user = mock(User.class);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userGameRepository.findAllByUserIdAndGameIdIn(
                userId,
                List.of(10L, 20L)
        )).willReturn(List.of());

        assertThatThrownBy(() ->
                userGameService.updateFavoriteGames(
                        userId,
                        List.of(10L, 20L)
                )
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(userFavoriteGameRepository, never())
                .deleteAllByUserId(userId);

        verify(userFavoriteGameRepository, never())
                .saveAll(anyList());
    }
}