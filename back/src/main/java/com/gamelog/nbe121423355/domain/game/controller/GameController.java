package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.GameSearchRequest;
import com.gamelog.nbe121423355.domain.game.service.GameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "game-controller", description = "게임 검색 및 목록 조회 API")
public class GameController {

    private final GameService gameService;

    @GetMapping("/suggestions")
    @Operation(summary = "게임 검색어 자동완성", description = "검색어와 일치하는 게임 후보를 최대 개수만큼 조회합니다.")
    public RsData<List<GameListResponse>> suggestions(
            @Parameter(description = "게임 제목 검색어", example = "Baldur")
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return new RsData<>("200-1", "게임 검색 후보를 조회했습니다.",
                gameService.getSuggestions(keyword));
    }

    // 페이징 목록 조회
    @GetMapping("/page")
    @Operation(summary = "게임 목록 조회", description = "검색어, 장르, 플랫폼과 정렬 조건으로 게임을 페이지 단위로 조회합니다.")
    public RsData<Page<GameListResponse>> page(
            @Valid @ModelAttribute GameSearchRequest request
    ) {
        Page<GameListResponse> responsePage =
                gameService.getGamesPage(request);

        return new RsData<>(
                "200-1",
                "게임 목록을 페이지 단위로 조회했습니다.",
                responsePage
        );
    }
}
