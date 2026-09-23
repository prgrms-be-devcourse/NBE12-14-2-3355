package com.gamelog.nbe121423355.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

@Schema(description = "게임 목록 검색 조건")
public record GameSearchRequest(
        @Schema(description = "페이지 번호(0부터 시작)", example = "0", defaultValue = "0", minimum = "0")
        @Min(0)
        Integer page,

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
        @Min(1)
        @Max(100)
        Integer size,

        @Schema(description = "정렬 기준", example = "LATEST", allowableValues = {"LATEST", "TITLE", "RATING", "LIBRARY", "PLAY_TIME"})
        GameSort sort,
        @Schema(description = "게임 제목 검색어", example = "Baldur", nullable = true)
        String keyword,
        @Schema(description = "장르 ID 목록", example = "[12, 31]", nullable = true)
        List<Long> genreIds,
        @Schema(description = "플랫폼 ID 목록", example = "[6, 48]", nullable = true)
        List<Long> platformIds
) {
    public GameSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    public Pageable toPageable() {
        Sort order = sort == null ? Sort.by("id").ascending() : switch (sort) {
            case LATEST -> Sort.by(Sort.Order.desc("releaseDate"), Sort.Order.asc("id"));
            case TITLE -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
            // 집계 정렬은 Repository에서 처리하고, 동점은 ID순으로 정렬합니다.
            case RATING, LIBRARY, PLAY_TIME -> Sort.by("id").ascending();
        };
        return org.springframework.data.domain.PageRequest.of(
                page, size, order
        );
    }
}
