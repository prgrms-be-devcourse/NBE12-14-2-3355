package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameFilterResponse;
import com.gamelog.nbe121423355.domain.game.service.GameFilterService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "game-filter-controller", description = "게임 검색 필터 API")
public class GameFilterController {
    private final GameFilterService gameFilterService;

    @GetMapping("/filters")
    @Operation(summary = "게임 필터 목록 조회", description = "검색 화면에서 사용할 전체 장르와 플랫폼 목록을 조회합니다.")
    public RsData<GameFilterResponse> filters() {
        return new RsData<>(
                "200-1",
                "게임 필터 목록을 조회했습니다.",
                gameFilterService.getFilters()
        );
    }
}
