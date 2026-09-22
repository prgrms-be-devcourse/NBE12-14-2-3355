package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.PopularGameResponse;
import com.gamelog.nbe121423355.domain.game.service.PopularGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class PopularGameController {

    private final PopularGameService popularGameService;

    @GetMapping("/popular")
    public RsData<List<PopularGameResponse>> getPopularGames(
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