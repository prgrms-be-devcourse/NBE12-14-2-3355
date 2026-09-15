package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class ApiV1UserController {

    private final UserService userService;

    @PostMapping ("/signup")
    public RsData<UserDto> signUp(
            @Valid
            @RequestBody SignupRequestDto signupRequestDto
    ) {
        return new RsData<>(
                "201-1",
                "회원가입 성공"
        );
    }

    @PostMapping("/login")
    public RsData<UserDto> login(
            @Valid
            @RequestBody LoginRequestDto loginRequestDto
    ){
        return new RsData<>(
                "200",
                "로그인 성공"
        );
    }

    @PostMapping("/refresh")
    public RsData<UserDto> refresh(
            @Valid
            @RequestBody LoginRequestDto loginRequestDto
    ){
        return new RsData<>(
                "200-1",
                "로그인 성공"
        );
    }
}
