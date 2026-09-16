package com.gamelog.nbe121423355.domain.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size
) {
    public PageRequest(Integer page, Integer size) {
        this.page = page == null ? 0 : page;
        this.size = size == null ? 20 : size;
    }

    public Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(
                page, size, Sort.by("id").ascending()
        );
    }
}
