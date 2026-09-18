package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameDetailResponse;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import com.gamelog.nbe121423355.domain.game.service.GameDetailService;
import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameDetailController {

    private final GameDetailService gameDetailService;

    // GameLog DB ID를 기준으로 게임 상세 정보 조회
    @GetMapping("/{gameId}")
    public RsData<GameDetailResponse> getGameDetail(
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
    public RsData<List<RelatedGameResponse>> getRelatedGames(
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
