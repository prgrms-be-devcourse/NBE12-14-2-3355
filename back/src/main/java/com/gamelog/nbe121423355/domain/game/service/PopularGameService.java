package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.PopularGameResponse;
import com.gamelog.nbe121423355.domain.game.repository.GameGenreRepository;
import com.gamelog.nbe121423355.domain.game.repository.GameStatisticsRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.PopularGameProjection;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularGameService {

    private static final int MAX_SIZE = 50;

    private final GameStatisticsRepository gameStatisticsRepository;
    private final GameGenreRepository gameGenreRepository;

    public List<PopularGameResponse> getPopularGames(int size) {
        validateSize(size);

        List<PopularGameProjection> popularGames =
                gameStatisticsRepository.findPopularGames(
                        PageRequest.of(0, size)
                );

        if (popularGames.isEmpty()) {
            return List.of();
        }

        List<Long> gameIds = popularGames.stream()
                .map(PopularGameProjection::getGameId)
                .toList();

        Map<Long, List<PopularGameResponse.GenreResponse>> genresByGameId =
                gameGenreRepository.findAllWithGenreByGameIdIn(gameIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                gameGenre -> gameGenre.getGame().getId(),
                                Collectors.mapping(
                                        gameGenre -> new PopularGameResponse.GenreResponse(
                                                gameGenre.getGenre().getId(),
                                                gameGenre.getGenre().getName()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        return popularGames.stream()
                .map(game -> new PopularGameResponse(
                        game.getGameId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getLikeCount(),
                        genresByGameId.getOrDefault(
                                game.getGameId(),
                                List.of()
                        )
                ))
                .toList();
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new ServiceException(
                    "400-9",
                    "조회 개수는 1 이상 50 이하여야 합니다."
            );
        }
    }
}