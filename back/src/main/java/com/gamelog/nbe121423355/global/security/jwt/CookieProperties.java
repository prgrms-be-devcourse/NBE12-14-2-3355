package com.gamelog.nbe121423355.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

// local: SameSite=Lax/secure=false, prod: SameSite=None/secure=true (cross-site 배포 대응)
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        String sameSite,
        boolean secure
) {
}
