package com.gamelog.nbe121423355.domain.usergame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserGameSearchRequest {
    @NotNull
    private UserGameTab status = UserGameTab.ALL;
    @NotNull
    private UserGameSort sort = UserGameSort.RECENT_PLAYED;
    @Size(max = 255)
    private String keyword;
    @Size(max = 100)
    private List<@NotNull @Positive Long> platformIds;
    @Size(max = 100)
    private List<@NotNull @Positive Long> genreIds;
    @Min(0)
    private int page = 0;
    @Min(1)
    @Max(100)
    private int size = 60;
}
