package com.gamelog.nbe121423355.domain.usergame.controller;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library/games")
@RequiredArgsConstructor
@Tag(name = "UserGameController", description = "유저 게임 라이브러리 API")
public class UserGameController {

    private UserGameService userGameService;

    @PostMapping("/{gameId}")
    public RsData<UserGameDto> addGameToLibrary(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody
            ){

        Long userId=1L;

        UserGame usergame = userGameService.addOrUpdateGameToLibrary(userId,gameId,reqBody);

        return new RsData<>(
                "201-1",
                "라이브러리에 게임을 등록했습니다.",
                new UserGameDto(usergame)
        );
    }

    @PatchMapping("/{gameId}")
    public RsData<UserGameDto> updatePlayRecord(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody
    ){
        Long userId=1L;

        UserGame userGame=userGameService.updatePlayRecord(
                userId, gameId, reqBody
        );

        return new RsData<>(
                "200-1",
                "게임 기록을 수정했습니다.",
                new UserGameDto(userGame)
        );
    }

}
