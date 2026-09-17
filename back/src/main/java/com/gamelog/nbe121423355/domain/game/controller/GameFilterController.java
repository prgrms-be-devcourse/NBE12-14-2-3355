package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameFilterResponse;
import com.gamelog.nbe121423355.domain.game.service.GameFilterService;
import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameFilterController {
    private final GameFilterService gameFilterService;

    @GetMapping("/filters")
    public RsData<GameFilterResponse> filters() {
        return new RsData<>(
                "200-1",
                "게임 필터 목록을 조회했습니다.",
                gameFilterService.getFilters()
        );
    }
}
