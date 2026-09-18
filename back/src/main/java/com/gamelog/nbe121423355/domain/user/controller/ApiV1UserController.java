package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.service.UserPreferenceGameService;
import com.gamelog.nbe121423355.domain.user.service.UserPreferenceGenreService;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import com.gamelog.nbe121423355.global.security.jwt.CookieProperties;
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
public class ApiV1UserController {

    private final UserService userService;
    private final UserPreferenceGenreService userPreferenceGenreService;
    private final UserPreferenceGameService userPreferenceGameService;
    private final CookieProperties cookieProperties;


    @PostMapping ("/signup")
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
    public RsData<LoginResponseDto> login(
            @Valid
            @RequestBody LoginRequestDto loginRequestDto,
                        HttpServletResponse httpServletResponse
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

    @PostMapping("/refresh")
    public RsData<TokenResponseDto> refresh(
            @CookieValue("refreshToken")
            String refreshToken
    ){
        TokenResponseDto tokenResponseDto = userService.refresh(refreshToken);
        return new RsData<>(
                "200-2",
                "토큰 재발급 성공",
                tokenResponseDto
        );
    }

    @GetMapping("/me")
    public RsData<UserDto> getMe(
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.getMe(securityUser.getId());
        return new RsData<>(
                "200-3",
                "내 정보 조회 성공",
                userDto
        );
    }

    @PatchMapping("/me")
    public RsData<UserDto> updateProfile(
            @AuthenticationPrincipal SecurityUser securityUser,
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
    public RsData<UserDto> onboarding(
        @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.exitOnboarding(securityUser.getId());
        return new RsData<>(
                "200-4",
                "온보딩 완료",
                userDto
        );
    }


    @PatchMapping("/me/onboarding/skip")
    public RsData<UserDto> onboardingSkip(
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        UserDto userDto = userService.skipOnboarding(securityUser.getId());
        return new RsData<>(
                "200-5",
                "온보딩 스킵",
                userDto
        );
    }

    @PutMapping("/me/preferred-genres")
    public RsData<List<PreferredGenreResponseDto>> updatePreferredGenres(
            @AuthenticationPrincipal SecurityUser securityUser,
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
    public RsData<List<PreferredGenreResponseDto>> getPreferredGenres(
            @AuthenticationPrincipal SecurityUser securityUser
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
    public RsData<List<PreferredGameResponseDto>> updatePreferredGames(
            @AuthenticationPrincipal SecurityUser securityUser,
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
    public RsData<List<PreferredGameResponseDto>> getPreferredGames(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<PreferredGameResponseDto> result =
                userPreferenceGameService.getPreferredGames(securityUser.getId());
        return new RsData<>(
                "200-9",
                "선호 게임 조회 성공",
                result
        );
    }

    @GetMapping("/check-email")
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
    public RsData<Boolean> chekNicknameDuplicate(
            @RequestParam String nickname
    ) {
        boolean result = userService.checkNicknameDuplicate(nickname);
        return new RsData<>(
                "200-12",
                "선호 게임 조회 성공",
                result
        );
    }
}
