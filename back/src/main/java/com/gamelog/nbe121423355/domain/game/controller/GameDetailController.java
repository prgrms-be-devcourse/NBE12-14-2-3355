package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import com.gamelog.nbe121423355.domain.game.service.GameDetailService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "game-detail-controller", description = "게임 상세 및 연관 게임 조회 API")
public class GameDetailController {

    private final GameDetailService gameDetailService;

    // GameLog DB ID를 기준으로 게임 상세 정보 조회
    @GetMapping("/{gameId}")
    @Operation(summary = "게임 상세 조회", description = "GameLog 데이터베이스 ID로 게임 정보와 커뮤니티 통계를 조회합니다.")
    public RsData<GameDetailResponse> getGameDetail(
            @Parameter(description = "GameLog 게임 ID", example = "6", required = true)
            @PathVariable("gameId") Long gameId
    ) {
        GameDetailResponse response = gameDetailService.getGameDetail(gameId);

        return new RsData<>(
                "200-1",
                "게임 상세 정보를 조회했습니다.",
                response
        );
    }

    // GameLog DB ID를 기준으로 연관 추천 게임 상위 5개 조회
    @GetMapping("/{gameId}/related")
    @Operation(summary = "연관 게임 조회", description = "장르와 시리즈를 기준으로 연관 게임을 최대 5개 조회합니다.")
    public RsData<List<RelatedGameResponse>> getRelatedGames(
            @Parameter(description = "GameLog 게임 ID", example = "6", required = true)
            @PathVariable("gameId") Long gameId
    ) {
        List<RelatedGameResponse> response = gameDetailService.getRelatedGames(gameId);

        return new RsData<>(
                "200-1",
                "연관 추천 게임을 조회했습니다.",
                response
        );
    }

}
