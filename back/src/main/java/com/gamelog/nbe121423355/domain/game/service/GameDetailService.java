package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.*;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDetailService {

    private final GameRepository gameRepository;
    private final GameGenreRepository gameGenreRepository;
    private final GamePlatformRepository gamePlatformRepository;
    private final GameSeriesGameRepository gameSeriesGameRepository;
    private final GameStatisticsRepository gameStatisticsRepository;
    private final RelatedGameQueryRepository relatedGameQueryRepository;

    // 게임 기본 정보와 연결 정보, 상태·평점·리뷰·좋아요 통계를 조회해 상세 응답으로 반환
    public GameDetailResponse getGameDetail(Long gameId) {
        // GameLog DB ID로 게임 기본 정보 조회
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new ServiceException(
                                "404-1",
                                "존재하지 않는 게임입니다."
                        )
                );

        // 게임에 연결된 장르 정보를 응답 DTO로 변환
        List<GameDetailResponse.GenreResponse> genres = gameGenreRepository
                .findAllWithGenreByGameId(gameId)
                .stream()
                .map(gameGenre -> new GameDetailResponse.GenreResponse(
                                gameGenre.getGenre().getId(),
                                gameGenre.getGenre().getName()
                        )
                )
                .toList();

        // 게임에 연결된 플랫폼 정보를 응답 DTO로 변환
        List<GameDetailResponse.PlatformResponse> platforms = gamePlatformRepository
                .findAllWithPlatformByGameId(gameId)
                .stream()
                .map(gamePlatform -> new GameDetailResponse.PlatformResponse(
                                gamePlatform.getPlatform().getId(),
                                gamePlatform.getPlatform().getName()
                        )
                )
                .toList();

        // 게임에 연결된 시리즈 정보를 응답 DTO로 변환
        List<GameDetailResponse.SeriesResponse> series = gameSeriesGameRepository
                .findAllWithSeriesByGameId(gameId)
                .stream()
                .map(seriesGame -> new GameDetailResponse.SeriesResponse(
                                seriesGame.getSeries().getId(),
                                seriesGame.getSeries().getName()
                        )
                )
                .toList();

        // 게임 ID를 기준으로 상태·평점·리뷰·좋아요 통계 조회
        GameStatusStatisticsProjection statusStatistics = gameStatisticsRepository.findStatusStatisticsByGameId(gameId);
        GameRatingStatisticsResponse ratingStatistics = getRatingStatistics(gameId);
        long likeCount = getLikeCount(gameId);
        GamePlayTimeStatisticsResponse playTimeStatistics = getPlayTimeStatistics(gameId);

        // 조회한 통계를 게임 상세 응답 DTO로 변환
        GameStatisticsResponse statistics = new GameStatisticsResponse(
                statusStatistics.getPlayedCount(),
                statusStatistics.getPlayingCount(),
                statusStatistics.getBacklogCount(),
                statusStatistics.getWishlistCount(),
                likeCount,
                ratingStatistics.averageRating(),
                ratingStatistics.reviewCount(),
                ratingStatistics.ratingDistribution(),
                playTimeStatistics.averagePlayTimeHours(),
                playTimeStatistics.playTimeUserCount()
        );

        return new GameDetailResponse(
                game.getId(),
                game.getIgdbId(),
                game.getTitle(),
                game.getCoverImageUrl(),
                game.getDeveloper(),
                game.getReleaseDate(),
                game.getDescription(),
                game.getIgdbRating(),
                genres,
                platforms,
                series,
                statistics
        );
    }

    // 기준 게임의 존재 여부를 확인한 뒤 연관 추천 게임 상위 5개를 반환
    public List<RelatedGameResponse> getRelatedGames(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new ServiceException(
                    "404-1",
                    "존재하지 않는 게임입니다."
            );
        }

        return relatedGameQueryRepository.findRelatedGames(gameId);
    }

    // 평균 평점, 리뷰 수와 모든 0.5점 단위의 평점 분포를 구성
    private GameRatingStatisticsResponse getRatingStatistics(Long gameId) {
        GameRatingStatisticsProjection ratingStatistics = gameStatisticsRepository
                .findRatingStatisticsByGameId(gameId);

        Map<Integer, Long> countsByRating = gameStatisticsRepository
                .findRatingDistributionByGameId(gameId)
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getRating()
                                .multiply(BigDecimal.valueOf(2))
                                .intValueExact(),
                        GameRatingDistributionProjection::getCount
                ));

        List<GameRatingDistributionResponse> distribution = IntStream.rangeClosed(1, 10)
                .mapToObj(ratingStep -> new GameRatingDistributionResponse(
                        BigDecimal.valueOf(ratingStep * 5L, 1),
                        countsByRating.getOrDefault(ratingStep, 0L)
                ))
                .toList();

        return new GameRatingStatisticsResponse(
                BigDecimal.valueOf(ratingStatistics.getAverageRating()),
                ratingStatistics.getReviewCount(),
                distribution
        );
    }

    // 게임에 좋아요를 누른 사용자 수 조회
    private long getLikeCount(Long gameId) {
        return gameStatisticsRepository.countLikesByGameId(gameId);
    }

    // 평균 플레이타임과 플레이타임 기록 사용자 수를 응답 DTO로 반환
    private GamePlayTimeStatisticsResponse getPlayTimeStatistics(Long gameId) {
        GamePlayTimeStatisticsProjection playTimeStatistics = gameStatisticsRepository
                .findPlayTimeStatisticsByGameId(gameId);

        return new GamePlayTimeStatisticsResponse(
                BigDecimal.valueOf(playTimeStatistics.getAveragePlayTimeHours()),
                playTimeStatistics.getPlayTimeUserCount()
        );
    }

}

