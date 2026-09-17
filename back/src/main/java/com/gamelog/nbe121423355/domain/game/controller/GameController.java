package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.PageRequest;
import com.gamelog.nbe121423355.domain.game.service.GameService;
import com.gamelog.nbe121423355.global.dto.RsData;
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
public class GameController {

    private final GameService gameService;

    @GetMapping
    public RsData<List<GameListResponse>> list(){
        List<GameListResponse> responseList=gameService.getGames();

        RsData<List<GameListResponse>> rsData=new RsData<>(
                "200-1",
                "게임 목록을 조회했습니다.",
                responseList
        );

        return rsData;
    }

    @GetMapping("/suggestions")
    public RsData<List<GameListResponse>> suggestions(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return new RsData<>("200-1", "게임 검색 후보를 조회했습니다.",
                gameService.getSuggestions(keyword));
    }

    // 페이징 목록 조회
    @GetMapping("/page")
    public RsData<Page<GameListResponse>> page(
            @Valid @ModelAttribute PageRequest request,

            @RequestParam(name = "keyword", required = false)
            String keyword,

            @RequestParam(name = "genreIds", required = false)
            List<Long> genreIds,

            @RequestParam(name = "platformIds", required = false)
            List<Long> platformIds
    ) {
        Page<GameListResponse> responsePage =
                gameService.getGamesPage(
                        keyword,
                        genreIds,
                        platformIds,
                        request.toPageable()
                );

        return new RsData<>(
                "200-1",
                "게임 목록을 페이지 단위로 조회했습니다.",
                responsePage
        );
    }
}
