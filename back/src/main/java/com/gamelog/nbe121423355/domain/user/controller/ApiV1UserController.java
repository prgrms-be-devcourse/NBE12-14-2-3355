package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.service.UserPreferenceGameService;
import com.gamelog.nbe121423355.domain.user.service.UserPreferenceGenreService;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import com.gamelog.nbe121423355.global.security.jwt.CookieProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "api-v1-user-controller", description = "회원가입·로그인·프로필·온보딩 API")
public class ApiV1UserController {

    private final UserService userService;
    private final UserPreferenceGenreService userPreferenceGenreService;
    private final UserPreferenceGameService userPreferenceGameService;
    private final CookieProperties cookieProperties;


    @PostMapping ("/signup")
    @Operation(summary = "회원가입", description = "닉네임, 이메일과 비밀번호로 일반 사용자 계정을 생성합니다.")
    public RsData<UserDto> signUp(
            @Valid
            @RequestBody SignupRequestDto signupRequestDto
    ) {
        UserDto userDto = userService.signUp(signupRequestDto);
        return new RsData<>(
                "201-1",
                "회원가입 성공",
                userDto
        );
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "accessToken을 응답하고 refreshToken을 HttpOnly 쿠키로 설정합니다.")
    public RsData<LoginResponseDto> login(
            @Valid
            @RequestBody LoginRequestDto loginRequestDto,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse
    ){
        UserService.LoginResult result = userService.login(loginRequestDto);

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite(cookieProperties.sameSite())
                .secure(cookieProperties.secure())
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        LoginResponseDto body = new LoginResponseDto(result.user(), result.accessToken());
        return new RsData<>(
                "200-1",
                "로그인 성공",
                body
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refreshToken을 폐기하고 인증 쿠키를 제거합니다.")
    public RsData<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse
    ) {
        if (refreshToken != null) {
            userService.logout(refreshToken);
        }

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite(cookieProperties.sameSite())
                .secure(cookieProperties.secure())
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        return new RsData<>("200-13", "로그아웃 성공");
    }

    @PostMapping("/refresh")
    @Operation(summary = "accessToken 재발급", description = "HttpOnly refreshToken 쿠키를 사용해 새로운 accessToken을 발급합니다.")
    public RsData<TokenResponseDto> refresh(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken
    ){
        TokenResponseDto tokenResponseDto = userService.refresh(refreshToken);
        return new RsData<>(
                "200-2",
                "토큰 재발급 성공",
                tokenResponseDto
        );
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 공개 정보 조회", description = "사용자 ID로 닉네임, 프로필 이미지와 자기소개를 조회합니다.")
    public RsData<PublicUserDto> getOtherUser(
            @PathVariable Long userId
    ){
        PublicUserDto userDto = userService.getUser(userId);
        return new RsData<>(
                "200-3",
                "유저 정보 조회 성공",
                userDto
        );
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserDto> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.getMe(securityUser.getId());
        return new RsData<>(
                "200-3",
                "내 정보 조회 성공",
                userDto
        );
    }

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "닉네임, 프로필 이미지와 자기소개를 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserDto> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateProfileRequestDto updateProfileRequestDto
    ) {
        UserDto userDto = userService.updateProfile(securityUser.getId(), updateProfileRequestDto);
        return new RsData<>(
                "200-10",
                "내 정보 수정 성공",
                userDto
        );
    }

    @PatchMapping("/me/onboarding")
    @Operation(summary = "온보딩 완료", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserDto> onboarding(
        @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.exitOnboarding(securityUser.getId());
        return new RsData<>(
                "200-4",
                "온보딩 완료",
                userDto
        );
    }


    @PatchMapping("/me/onboarding/skip")
    @Operation(summary = "온보딩 건너뛰기", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserDto> onboardingSkip(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.skipOnboarding(securityUser.getId());
        return new RsData<>(
                "200-5",
                "온보딩 스킵",
                userDto
        );
    }

    @PutMapping("/me/preferred-genres")
    @Operation(summary = "선호 장르 저장", description = "온보딩에서 선택한 선호 장르 목록을 교체합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<List<PreferredGenreResponseDto>> updatePreferredGenres(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody PreferredGenreRequestDto requestDto
    ) {
        List<PreferredGenreResponseDto> result =
                userPreferenceGenreService.updatePreferredGenres(securityUser.getId(), requestDto.genreIds());
        return new RsData<>(
                "200-6",
                "선호 장르 저장 성공",
                result
        );
    }

    @GetMapping("/me/preferred-genres")
    @Operation(summary = "선호 장르 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<List<PreferredGenreResponseDto>> getPreferredGenres(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<PreferredGenreResponseDto> result =
                userPreferenceGenreService.getPreferredGenres(securityUser.getId());
        return new RsData<>(
                "200-7",
                "선호 장르 조회 성공",
                result
        );
    }

    @PutMapping("/me/preferred-games")
    @Operation(summary = "선호 게임 저장", description = "온보딩에서 선택한 선호 게임 목록을 교체합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<List<PreferredGameResponseDto>> updatePreferredGames(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody PreferredGameRequestDto requestDto
    ) {
        List<PreferredGameResponseDto> result =
                userPreferenceGameService.updatePreferredGames(securityUser.getId(), requestDto.gameIds());
        return new RsData<>(
                "200-8",
                "선호 게임 저장 성공",
                result
        );
    }

    @GetMapping("/me/preferred-games")
    @Operation(summary = "선호 게임 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<List<PreferredGameResponseDto>> getPreferredGames(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<PreferredGameResponseDto> result =
                userPreferenceGameService.getPreferredGames(securityUser.getId());
        return new RsData<>(
                "200-9",
                "선호 게임 조회 성공",
                result
        );
    }

    @PatchMapping("/me/password")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto
    ) {
        userService.changePassword(securityUser.getId(), changePasswordRequestDto);
        return new RsData<>(
                "200-14",
                "비밀번호 변경 성공"
        );
    }

    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "true이면 이미 사용 중인 이메일입니다.")
    public RsData<Boolean> checkEmailDuplicate(
            @RequestParam String email
    ) {
        boolean result = userService.checkEmailDuplicate(email);
        return new RsData<>(
                "200-11",
                "이메일 중복 확인 성공",
                result
        );
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인", description = "true이면 이미 사용 중인 닉네임입니다.")
    public RsData<Boolean> chekNicknameDuplicate(
            @RequestParam String nickname
    ) {
        boolean result = userService.checkNicknameDuplicate(nickname);
        return new RsData<>(
                "200-12",
                "닉네임 중복 확인 성공",
                result
        );
    }


}
