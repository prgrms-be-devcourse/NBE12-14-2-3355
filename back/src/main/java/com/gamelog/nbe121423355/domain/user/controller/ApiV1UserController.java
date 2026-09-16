package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class ApiV1UserController {

    private final UserService userService;

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
        UserDto userDto = userService.exitOnboarding(securityUser.getId());
        return new RsData<>(
                "200-5",
                "온보딩 스킵",
                userDto
        );
    }
}
