package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameStatisticsResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDetailService {

    private final GameRepository gameRepository;
    private final GameGenreRepository gameGenreRepository;
    private final GamePlatformRepository gamePlatformRepository;
    private final GameSeriesGameRepository gameSeriesGameRepository;
    private final GameStatisticsRepository gameStatisticsRepository;

    // 게임 기본 정보와 장르·플랫폼·시리즈·상태 통계를 조회해 상세 응답으로 반환
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

        // 게임 ID를 기준으로 Played, Playing, Backlog, Wishlist 인원수 조회
        GameStatusStatisticsProjection statusStatistics = gameStatisticsRepository.findStatusStatisticsByGameId(gameId);

        // 상태 통계 조회 결과를 최종 API 응답 DTO로 변환
        GameStatisticsResponse statistics = new GameStatisticsResponse(
                statusStatistics.getPlayedCount(),
                statusStatistics.getPlayingCount(),
                statusStatistics.getBacklogCount(),
                statusStatistics.getWishlistCount()
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

}
