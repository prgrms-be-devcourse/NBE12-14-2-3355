package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(GameSearchRequest request) {
        String keyword = request.keyword();
        List<Long> genreIds = request.genreIds();
        List<Long> platformIds = request.platformIds();
        String keywordPattern =
                keyword == null || keyword.isBlank()
                        ? null
                        : toContainsPattern(keyword.strip());

        boolean filterGenres =
                genreIds != null && !genreIds.isEmpty();

        boolean filterPlatforms =
                platformIds != null && !platformIds.isEmpty();

        // 검색어가 있고 필터가 없는 기본 정렬 검색은 ID를 먼저 조회합니다.
        boolean useDefaultSearch =
                keywordPattern != null
                        && !filterGenres
                        && !filterPlatforms
                        && request.sort() == null;

        if (useDefaultSearch) {
            var pageable = PageRequest.of(request.page(), request.size());
            var minDate = LocalDate.of(1970, 1, 1);
            var maxDate = LocalDate.now().withDayOfYear(1).plusYears(1);
            var content = gameRepository.findDefaultSearchContent(
                    keywordPattern, minDate, maxDate, pageable.getPageSize(), pageable.getOffset());
            // 마지막 부분 페이지는 크기로 총 개수를 알 수 있습니다.
            // 범위를 벗어난 빈 페이지는 COUNT를 실행하여 실제 총 개수를 유지합니다.
            return PageableExecutionUtils.getPage(content, pageable,
                            () -> gameRepository.countDefaultSearch(keywordPattern, minDate, maxDate))
                    .map(GameListResponse::new);
        }

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
                        request.sort() == null ? "" : request.sort().name(),
                        LocalDate.of(1970, 1, 1),
                        LocalDate.now().withDayOfYear(1).plusYears(1),
                        request.toPageable()
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
