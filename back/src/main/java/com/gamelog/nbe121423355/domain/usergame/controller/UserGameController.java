package com.gamelog.nbe121423355.domain.usergame.controller;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library/games")
@RequiredArgsConstructor
@Tag(name = "UserGameController", description = "유저 게임 라이브러리 API")
public class UserGameController {

    private final UserGameService userGameService;

    //등록 및 수정
    @PostMapping("/{gameId}")
    public RsData<UserGameDto> addGameToLibrary(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody,
            @AuthenticationPrincipal SecurityUser user
            ){

        System.out.println("user = " + user);

        Long userId = user.getId();
        UserGameSaveResult result = userGameService.addOrUpdateGameToLibrary(userId,gameId,reqBody);

        String message = result.created()
                ? "라이브러리에 게임을 등록했습니다."
                : "게임 기록을 수정했습니다.";

        String code = result.created()
                ? "201-1"
                : "200-1";

        return new RsData<>(
                code,
                message,
                new UserGameDto(result.userGame())
        );
    }

    //수정만
    @PatchMapping("/{gameId}")
    public RsData<UserGameDto> updatePlayRecord(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody,
            @AuthenticationPrincipal SecurityUser user
    ){

        Long userId = user.getId();
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
