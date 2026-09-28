package com.gamelog.nbe121423355.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "GameLog API",
                version = "v1",
                description = "게임 기록, 리뷰, 좋아요와 신고 기능을 제공하는 GameLog REST API입니다.",
                contact = @Contact(name = "GameLog Team")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "로그인 후 발급받은 accessToken을 입력하세요. Bearer 접두사는 자동으로 붙습니다."
)
public class OpenApiConfig {
}

