package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class UserGameServiceProfileTest {
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

    @Mock
    private UserFollowRepository userFollowRepository;

    @Test
    @DisplayName("비로그인 사용자는 공개 프로필을 조회하고 팔로우 상태는 조회하지 않는다")
    void anonymousProfile() {
        // given: 조회 대상 사용자가 존재
        when(userRepository.existsById(2L)).thenReturn(true);
        // when: 비로그인 상태로 조회
        UserProfileResponse result = userGameService.profileTab(2L, null);
        // then: 대상 사용자와 공개 조회 상태를 반환
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.isMe()).isFalse();
        assertThat(result.isFollowing()).isFalse();
        verifyNoInteractions(userFollowRepository);
    }

    @Test
    @DisplayName("다른 사용자 프로필에서 현재 사용자의 팔로우 여부를 반환한다")
    void otherUserProfile() {
        // given: 현재 사용자가 조회 대상을 팔로우
        when(userRepository.existsById(2L)).thenReturn(true);
        when(userFollowRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);
        // when: 다른 사용자의 프로필을 조회
        UserProfileResponse result = userGameService.profileTab(2L, 1L);
        // then: 본인이 아니며 팔로우 중인 것으로 반환
        assertThat(result.isMe()).isFalse();
        assertThat(result.isFollowing()).isTrue();
        verify(userGameRepository).findPlayedGames(2L);
    }

    @Test
    @DisplayName("존재하지 않는 프로필은 조회할 수 없다")
    void missingProfile() {
        // given: 저장되지 않은 사용자 ID를 준비
        Long missingId = 999L;
        // when: 없는 사용자의 프로필 조회를 요청
        // then: 통계 조회 전에 예외를 반환
        assertThatThrownBy(() -> userGameService.profileTab(missingId, null))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(userGameRepository, userFollowRepository);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 공개 게임 목록은 조회할 수 없다")
    void missingPublicGameList() {
        // given: 존재하지 않는 사용자와 기본 검색 조건을 준비
        Long missingUserId = 999L;
        UserGameSearchRequest request = new UserGameSearchRequest();

        // when: 없는 사용자의 공개 게임 목록을 조회
        // then: 목록 쿼리 실행 전에 사용자 없음 예외를 반환
        assertThatThrownBy(() ->
                userGameService.getPublicUserGameList(
                        missingUserId,
                        request
                )
        ).isInstanceOf(ServiceException.class);
        verifyNoInteractions(userGameRepository);
    }

    @Test
    @DisplayName("프로필 탭 조회 - 플레이 요약과 취향 분석을 정상적으로 반환한다")
    void t21() {
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
        assertThat(response.tasteResponse().rating().message())
                .isEqualTo("게임을 비교적 후하게 평가하는 편이에요.");
    }

    @Test
    @DisplayName("완료 비율이 40% 이상 70% 미만이면 절반 정도를 완료했다고 판단한다")
    void t28() {
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

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

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of(
                        new UserGameGenreDTO(1L, "RPG", 2L),
                        new UserGameGenreDTO(2L, "Action", 1L)
                ));

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
        // given: 프로필 통계 데이터를 준비
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        mockProfileRepositories(
                userId,
                createPlayedGames(3)
        );

        when(userGameRepository.findGenreDistribution(userId))
                .thenReturn(List.of());

        // when: 프로필을 조회
        UserProfileResponse response =
                userGameService.profileTab(userId, userId);

        // then: 통계와 취향 분석 결과를 검증
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
}
