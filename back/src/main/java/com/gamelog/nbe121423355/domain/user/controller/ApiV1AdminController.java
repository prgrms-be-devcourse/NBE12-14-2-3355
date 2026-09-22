package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class ApiV1AdminController {

    private final UserService userService;

    @PatchMapping("/users/{userId}/role")
    public RsData<UserDto> promoteToAdmin(
            @PathVariable Long userId,
            @AuthenticationPrincipal SecurityUser adminUser

    ) {
        UserDto result =userService.promoteToAdmin(userId);
        return new RsData<>(
                "200-13",
                "관리자로 승격 성공",
                result
        );
    }
}
