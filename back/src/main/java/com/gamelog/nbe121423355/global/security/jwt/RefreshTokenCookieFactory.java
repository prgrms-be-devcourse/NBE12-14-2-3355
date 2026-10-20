package com.gamelog.nbe121423355.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// refreshToken 쿠키 생성 (로그인, 로그아웃, /refresh, 자동 재발급 필터에서 공통 사용)
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";

    private final CookieProperties cookieProperties;

    // maxAge = 세션 만료까지 남은 시간 (Rotation돼도 로그인 시점 기준 절대 만료라 점점 줄어듦)
    // expiryDate는 ZoneId.systemDefault() 기준으로 저장되므로 비교도 LocalDateTime.now()
    public ResponseCookie create(String refreshToken, LocalDateTime expiryDate) {
        Duration maxAge = Duration.between(LocalDateTime.now(), expiryDate);
        return build(refreshToken, maxAge.isNegative() ? Duration.ZERO : maxAge);
    }

    // 쿠키 삭제용 (로그아웃)
    public ResponseCookie delete() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieProperties.sameSite())
                .secure(cookieProperties.secure())
                .build();
    }
}
