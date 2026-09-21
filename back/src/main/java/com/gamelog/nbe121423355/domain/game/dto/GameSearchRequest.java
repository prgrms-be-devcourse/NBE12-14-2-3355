package com.gamelog.nbe121423355.domain.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

public record GameSearchRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        GameSort sort,
        String keyword,
        List<Long> genreIds,
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
