package com.gamelog.nbe121423355.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

// local: SameSite=Lax/secure=false, prod: SameSite=Lax/secure=true (Next.js 프록시 경유)
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        String sameSite,
        boolean secure
) {
}
