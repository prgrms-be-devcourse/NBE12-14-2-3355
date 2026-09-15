package com.gamelog.nbe121423355.domain.usergame.controller;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/library/games")
@RequiredArgsConstructor
@Tag(name = "UserGameController", description = "유저 게임 라이브러리 API")
public class UserGameController {

    private UserGameService userGameService;

    public record UserGameReqBody(
            PlayStatus playStatus,
            Boolean isPlaying,
            Boolean isBacklog,
            Boolean isWishlist,
            Boolean isLiked,
            Long platformId,

            @PositiveOrZero
            BigDecimal playTimeHours,

            @PositiveOrZero
            BigDecimal finishTimeHours,

            @PositiveOrZero
            BigDecimal masterTimeHours,

            LocalDate startedAt,
            LocalDate completedAt,
            LocalDateTime lastPlayedAt
    ){}

    @PostMapping("/{gameId}")
    private RsData<UserGameDto> addGameToLibrary(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody
            ){

        UserGame usergame = userGameService.addGameToLibrary(userId,gameId,reqBody);

        return new RsData<>(
                "201-1",
                "라이브러리에 게임을 등록했습니다.",
                new UserGameDto(usergame)
        );
    }
}
