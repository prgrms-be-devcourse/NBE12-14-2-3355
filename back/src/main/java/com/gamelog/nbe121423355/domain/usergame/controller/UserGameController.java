package com.gamelog.nbe121423355.domain.usergame.controller;

import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library/games")
@RequiredArgsConstructor
@Tag(name = "UserGameController", description = "유저 게임 라이브러리 API")
public class UserGameController {

    private final UserGameService userGameService;

    @GetMapping("")
    public RsData<UserGameLibraryResponse> getUserGameList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "60") int size,
            @AuthenticationPrincipal SecurityUser user
    ){
        Pageable pageable = PageRequest.of(page, size);
        Page<UserGameListResponse> pages =
                userGameService.getUserGameList(user.getId(), pageable);

        return new RsData<>(
                "200-1",
                "라이브러리 게임 목록을 조회했습니다.",
                new UserGameLibraryResponse(
                        pages.getTotalPages(),
                        pages.getContent()
                )
        );
    }

    //등록 및 수정
    @PostMapping("/{gameId}")
    public RsData<UserGameDto> addGameToLibrary(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody,
            @AuthenticationPrincipal SecurityUser user
            ){

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

    @PutMapping("/{gameId}/play-status")
    public RsData<UserGamePlayStatusResponse> changePlayStatus(
            @PathVariable Long gameId,
            @RequestParam(required = false) PlayStatus status,
            @AuthenticationPrincipal SecurityUser user
    ) {
        PlayStatus playStatus = userGameService.changePlayed(user.getId(), gameId, status);

        return new RsData<>(
                "200-1",
                "플레이 상태를 변경했습니다.",
                new UserGamePlayStatusResponse(playStatus)
        );
    }

    @PatchMapping("/{gameId}/playing")
    public RsData<UserGameStatusResponse> changePlaying(
            @PathVariable Long gameId,
            @RequestParam boolean playing,
            @AuthenticationPrincipal SecurityUser user
    ) {
        boolean playingStatus = userGameService.changePlaying(user.getId(), gameId, playing);

        return new RsData<>(
                "200-1",
                "플레이 중 상태를 변경했습니다.",
                new UserGameStatusResponse(playingStatus)
        );
    }

    @PatchMapping("/{gameId}/wishlist")
    public RsData<UserGameStatusResponse> changeWishlist(
            @PathVariable Long gameId,
            @RequestParam boolean wishlist,
            @AuthenticationPrincipal SecurityUser user
    ) {
        boolean wishlistStatus = userGameService.changeWishlist(user.getId(), gameId,wishlist);

        return new RsData<>(
                "200-1",
                "위시리스트 상태를 변경했습니다.",
                new UserGameStatusResponse(wishlistStatus)
        );
    }

    @PatchMapping("/{gameId}/backlog")
    public RsData<UserGameStatusResponse> changeBacklog(
            @PathVariable Long gameId,
            @RequestParam boolean backlog,
            @AuthenticationPrincipal SecurityUser user
    ) {
        boolean backlogStatus = userGameService.changeBacklog(user.getId(), gameId, backlog);

        return new RsData<>(
                "200-1",
                "백로그 상태를 변경했습니다.",
                new UserGameStatusResponse(backlogStatus)
        );
    }

    @PatchMapping("/{gameId}/liked")
    public RsData<UserGameStatusResponse> changeLiked(
            @PathVariable Long gameId,
            @RequestParam boolean liked,
            @AuthenticationPrincipal SecurityUser user
    ) {
        boolean likedStatus = userGameService.changeLiked(user.getId(), gameId, liked);

        return new RsData<>(
                "200-1",
                "좋아요 상태를 변경했습니다.",
                new UserGameStatusResponse(likedStatus)
        );
    }

    @GetMapping("/profile")
    public RsData<UserProfileResponse> profileTab(
            @AuthenticationPrincipal SecurityUser user
    ){
        UserProfileResponse response = userGameService.profileTab(user.getId());

        return new RsData<>(
                "200-1",
                "프로필 정보를 조회했습니다.",
                response
        );
    }

}
