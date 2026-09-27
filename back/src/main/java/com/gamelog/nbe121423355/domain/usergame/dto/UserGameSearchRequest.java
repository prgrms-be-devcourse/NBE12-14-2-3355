package com.gamelog.nbe121423355.domain.usergame.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "라이브러리 상태 필터", example = "ALL", defaultValue = "ALL")
    @NotNull
    private UserGameTab status = UserGameTab.ALL;
    @Schema(description = "정렬 기준", example = "RECENT_PLAYED", defaultValue = "RECENT_PLAYED")
    @NotNull
    private UserGameSort sort = UserGameSort.RECENT_PLAYED;
    @Schema(description = "게임 제목 검색어", example = "Baldur", nullable = true)
    @Size(max = 255)
    private String keyword;
    @Schema(description = "플랫폼 ID 필터", example = "[1, 6]", nullable = true)
    @Size(max = 100)
    private List<@NotNull @Positive Long> platformIds;
    @Schema(description = "장르 ID 필터", example = "[5, 12]", nullable = true)
    @Size(max = 100)
    private List<@NotNull @Positive Long> genreIds;
    @Schema(description = "페이지 번호(0부터 시작)", example = "0", defaultValue = "0", minimum = "0")
    @Min(0)
    private int page = 0;
    @Schema(description = "페이지 크기", example = "20", defaultValue = "60", minimum = "1", maximum = "100")
    @Min(1)
    @Max(100)
    private int size = 60;
}
