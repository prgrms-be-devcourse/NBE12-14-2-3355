package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.PopularGameResponse;
import com.gamelog.nbe121423355.domain.game.service.PopularGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "popular-game-controller", description = "인기 게임 조회 API")
public class PopularGameController {

    private final PopularGameService popularGameService;

    @GetMapping("/popular")
    @Operation(summary = "인기 게임 조회", description = "좋아하는 게임 등록 수가 많은 순서로 게임을 조회합니다.")
    public RsData<List<PopularGameResponse>> getPopularGames(
            @Parameter(description = "조회할 게임 수", example = "5")
            @RequestParam(defaultValue = "5") int size
    ) {
        List<PopularGameResponse> response =
                popularGameService.getPopularGames(size);

        return new RsData<>(
                "200-1",
                "인기 게임을 조회했습니다.",
                response
        );
    }
}
