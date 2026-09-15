package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameImportService {

    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final GameGenreRepository gameGenreRepository;
    private final GamePlatformRepository gamePlatformRepository;
    private final GameSeriesRepository gameSeriesRepository;
    private final GameSeriesGameRepository gameSeriesGameRepository;

    @Transactional
    public int saveGames(List<IgdbGameResponse> responses) {
        for (IgdbGameResponse response : responses) {
            Game game = gameRepository.findByIgdbId(response.id())
                    .orElse(null);

            if (game == null) {
                // 저장 후 ID가 부여된 객체를 사용합니다.
                game = gameRepository.save(Game.createFromIgdb(response));
            } else {
                game.updateFromIgdb(response);
            }

            saveGenres(game, response.genres());
            savePlatforms(game, response.platforms());
            saveSeries(game, response.collections());
        }

        return responses.size();
    }

    private void saveGenres(
            Game game,
            List<IgdbGameResponse.NamedResource> genres
    ) {
        if (genres == null) {
            return;
        }

        for (IgdbGameResponse.NamedResource response : genres) {
            Genre genre = genreRepository
                    .findByIgdbGenreId(response.id())
                    .orElseGet(() -> genreRepository.save(
                            Genre.createFromIgdb(response.id(), response.name())
                    ));

            GameGenre link = new GameGenre(game, genre);

            if (!gameGenreRepository.existsById(link.getId())) {
                gameGenreRepository.save(link);
            }
        }
    }

    private void savePlatforms(
            Game game,
            List<IgdbGameResponse.NamedResource> platforms
    ) {
        if (platforms == null) {
            return;
        }

        for (IgdbGameResponse.NamedResource response : platforms) {
            Platform platform = platformRepository
                    .findByIgdbPlatformsId(response.id())
                    .orElseGet(() -> platformRepository.save(
                            Platform.createFromIgdb(
                                    response.id(), response.name()
                            )
                    ));

            GamePlatform link = new GamePlatform(game, platform);

            if (!gamePlatformRepository.existsById(link.getId())) {
                gamePlatformRepository.save(link);
            }
        }
    }

    private void saveSeries(
            Game game,
            List<IgdbGameResponse.NamedResource> collections
    ) {
        if (collections == null) {
            return;
        }

        for (IgdbGameResponse.NamedResource response : collections) {
            GameSeries series = gameSeriesRepository
                    .findByIgdbId(response.id())
                    .orElseGet(() -> gameSeriesRepository.save(
                            GameSeries.createFromIgdb(
                                    response.id(), response.name()
                            )
                    ));

            // 기존 생성자의 인자 순서는 시리즈, 게임입니다.
            GameSeriesGame link = new GameSeriesGame(series, game);

            if (!gameSeriesGameRepository.existsById(link.getId())) {
                gameSeriesGameRepository.save(link);
            }
        }
    }
}
