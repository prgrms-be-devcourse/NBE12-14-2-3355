package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.service.UserService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@Tag(name = "api-v1-admin-controller", description = "관리자 권한 관리 API")
public class ApiV1AdminController {

    private final UserService userService;

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "사용자 관리자 승격", description = "ADMIN 권한으로 일반 사용자를 관리자로 승격합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<UserDto> promoteToAdmin(
            @Parameter(description = "관리자로 승격할 사용자 ID", example = "2")
            @PathVariable Long userId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUser adminUser

    ) {
        UserDto result =userService.promoteToAdmin(userId);
        // 권한 변경 이력 추적용 (누가 누구를 승격했는지)
        log.info("관리자 승격: adminId={}, targetUserId={}", adminUser.getId(), userId);
        return new RsData<>(
                "200-13",
                "관리자로 승격 성공",
                result
        );
    }
}
