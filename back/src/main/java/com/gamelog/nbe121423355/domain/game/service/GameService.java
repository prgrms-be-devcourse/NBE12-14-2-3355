package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameSort;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final IgdbClient igdbClient;
    private final GameImportService gameImportService;
    private final GameRepository gameRepository;

    public List<IgdbGameResponse> fetchGamesFromIgdb() {
        return igdbClient.fetchGames();
    }

    public int importGames() {
        List<IgdbGameResponse> games = fetchGamesFromIgdb();

        return gameImportService.saveGames(games);
    }

    // 기존 페이징 호출 유지
    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(Pageable pageable) {
        return getGamesPage(null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(
            String keyword,
            Pageable pageable
    ) {
        return getGamesPage(keyword, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(
            String keyword,
            List<Long> genreIds,
            Pageable pageable
    ) {
        return getGamesPage(keyword, genreIds, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(
            String keyword,
            List<Long> genreIds,
            List<Long> platformIds,
            Pageable pageable
    ) {
        return getGamesPage(keyword, genreIds, platformIds, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(String keyword, List<Long> genreIds,
            List<Long> platformIds, Pageable pageable, GameSort sort) {
        String keywordPattern =
                keyword == null || keyword.isBlank()
                        ? null
                        : toContainsPattern(keyword.strip());

        boolean filterGenres =
                genreIds != null && !genreIds.isEmpty();

        boolean filterPlatforms =
                platformIds != null && !platformIds.isEmpty();

        // 필터 미선택 시에도 IN 절에는 비어 있지 않은 목록을 전달합니다.
        // 해당 조건은 filterGenres/filterPlatforms가 false이면 무시됩니다.
        List<Long> queryGenreIds =
                filterGenres ? genreIds : List.of(0L);

        List<Long> queryPlatformIds =
                filterPlatforms ? platformIds : List.of(0L);

        return gameRepository.findByFilters(
                        keywordPattern,
                        filterGenres,
                        queryGenreIds,
                        filterPlatforms,
                        queryPlatformIds,
                        sort == null ? "" : sort.name(),
                        pageable
                )
                .map(GameListResponse::new);
    }

    @Transactional(readOnly = true)
    public List<GameListResponse> getSuggestions(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String normalized = keyword.strip();
        String escaped = escapeLike(normalized);
        return gameRepository.findSuggestions(normalized, escaped + "%", "%" + escaped + "%",
                        org.springframework.data.domain.PageRequest.of(0, 6))
                .stream().map(GameListResponse::new).toList();
    }

    private String escapeLike(String keyword) {
        return keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

    }

    private String toContainsPattern(String keyword) {
        return "%" + escapeLike(keyword) + "%";
    }

}
