package com.gamelog.nbe121423355.domain.usergame.controller;

import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/library/games")
@RequiredArgsConstructor
@Tag(name = "user-game-controller", description = "내 게임 라이브러리·플레이 상태·프로필 기록 API")
public class UserGameController {

    private final UserGameService userGameService;

    @GetMapping("")
    @Operation(summary = "내 게임 목록 조회", description = "상태, 플랫폼, 장르, 검색어와 정렬 조건으로 내 게임 목록을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameLibraryResponse> getUserGameList(
            @Valid @ModelAttribute UserGameSearchRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ){
        Page<UserGameListResponse> pages =
                userGameService.getUserGameList(user.getId(), request);

        return new RsData<>(
                "200-1",
                "라이브러리 게임 목록을 조회했습니다.",
                new UserGameLibraryResponse(
                        pages.getTotalElements(),
                        pages.getTotalPages(),
                        pages.getContent()
                )
        );
    }

    @GetMapping("/profile/{userId}/games")
    @Operation(summary = "사용자 프로필 게임 목록 조회", description = "특정 사용자의 공개 게임 기록을 페이지 단위로 조회합니다.")
    public RsData<UserGameLibraryResponse> getUserProfileGames(
            @PathVariable Long userId,
            @Valid @ModelAttribute UserGameSearchRequest request
    ){
        Page<UserGameListResponse> pages =
                userGameService.getPublicUserGameList(userId, request);

        return new RsData<>(
                "200-1",
                "라이브러리 게임 목록을 조회했습니다.",
                new UserGameLibraryResponse(
                        pages.getTotalElements(),
                        pages.getTotalPages(),
                        pages.getContent()
                )
        );
    }

    //등록 및 수정
    @PostMapping("/{gameId}")
    @Operation(summary = "게임 기록 등록 또는 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameDto> addGameToLibrary(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
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
    @Operation(summary = "게임 기록 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameDto> updatePlayRecord(
            @PathVariable Long gameId,
            @Valid @RequestBody UserGameReqBody reqBody,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
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
    @Operation(summary = "플레이 완료 상태 변경", description = "PLAYED, COMPLETED, RETIRED, SHELVED, DROPPED 중 하나를 저장하며, 값을 생략하면 상태를 해제합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGamePlayStatusResponse> changePlayStatus(
            @PathVariable Long gameId,
            @RequestParam(required = false) PlayStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        PlayStatus playStatus = userGameService.changePlayed(user.getId(), gameId, status);

        return new RsData<>(
                "200-1",
                "플레이 상태를 변경했습니다.",
                new UserGamePlayStatusResponse(playStatus)
        );
    }

    @PatchMapping("/{gameId}/playing")
    @Operation(summary = "플레이 중 상태 변경", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameStatusResponse> changePlaying(
            @PathVariable Long gameId,
            @RequestParam boolean playing,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        boolean playingStatus = userGameService.changePlaying(user.getId(), gameId, playing);

        return new RsData<>(
                "200-1",
                "플레이 중 상태를 변경했습니다.",
                new UserGameStatusResponse(playingStatus)
        );
    }

    @PatchMapping("/{gameId}/wishlist")
    @Operation(summary = "위시리스트 상태 변경", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameStatusResponse> changeWishlist(
            @PathVariable Long gameId,
            @RequestParam boolean wishlist,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        boolean wishlistStatus = userGameService.changeWishlist(user.getId(), gameId,wishlist);

        return new RsData<>(
                "200-1",
                "위시리스트 상태를 변경했습니다.",
                new UserGameStatusResponse(wishlistStatus)
        );
    }

    @PatchMapping("/{gameId}/backlog")
    @Operation(summary = "플레이 예정 상태 변경", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameStatusResponse> changeBacklog(
            @PathVariable Long gameId,
            @RequestParam boolean backlog,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        boolean backlogStatus = userGameService.changeBacklog(user.getId(), gameId, backlog);

        return new RsData<>(
                "200-1",
                "백로그 상태를 변경했습니다.",
                new UserGameStatusResponse(backlogStatus)
        );
    }

    @PatchMapping("/{gameId}/liked")
    @Operation(summary = "좋아하는 게임 상태 변경", description = "프로필 Likes 탭에 표시할 게임의 하트 상태를 변경합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserGameStatusResponse> changeLiked(
            @PathVariable Long gameId,
            @RequestParam boolean liked,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        boolean likedStatus = userGameService.changeLiked(user.getId(), gameId, liked);

        return new RsData<>(
                "200-1",
                "좋아요 상태를 변경했습니다.",
                new UserGameStatusResponse(likedStatus)
        );
    }

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 게임 통계 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserProfileResponse> profileTab(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ){
        UserProfileResponse response = userGameService.profileTab(user.getId(), user.getId());

        return new RsData<>(
                "200-1",
                "프로필 정보를 조회했습니다.",
                response
        );
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "사용자 프로필 게임 통계 조회", description = "특정 사용자의 공개 프로필 기록과 게임 통계를 조회합니다.")
    public RsData<UserProfileResponse> getProfile(
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        Long currentUserId =
                user != null ? user.getId() : null;

        UserProfileResponse response =
                userGameService.profileTab(userId, currentUserId);

        return new RsData<>(
                "200-1",
                "프로필을 조회했습니다.",
                response
        );
    }

    @PutMapping("/favorite-games")
    @Operation(summary = "인생 게임 수정", description = "프로필에 노출할 인생 게임 목록과 순서를 저장합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<List<UserFavoriteGameResponse>> updateFavoriteGames(
            @RequestBody UserFavoriteGameRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser user
    ) {
        List<UserFavoriteGameResponse> response = userGameService.updateFavoriteGames(
                user.getId(),
                request.gameIds()
        );

        return new RsData<>(
                "200-1",
                "인생게임을 수정했습니다.",
                response
        );
    }
}
