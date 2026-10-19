package com.gamelog.nbe121423355.global.security.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "2yFvv+amd1bJKWDQ/picSaxbhQ+VKrMIjFqIqbBqib8=";

    private JwtProvider jwtProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 3600000L, 1209600000L));
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    @DisplayName("isExpiredAccessToken - 아직 만료되지 않은 accessToken이면 false")
    void isExpiredAccessToken_false_whenNotExpired() {
        String token = jwtProvider.generateAccessToken(1L, "USER");

        assertThat(jwtProvider.isExpiredAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("isExpiredAccessToken - 시간이 지나 만료된 accessToken이면 true")
    void isExpiredAccessToken_true_whenExpired() {
        String expiredToken = createToken(secretKey, "Access", expiredIssuedAt(), expiredExpiration());

        assertThat(jwtProvider.isExpiredAccessToken(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("isExpiredAccessToken - 만료된 refreshToken이면 false (refreshToken으로 재발급 경로 진입 차단)")
    void isExpiredAccessToken_false_whenExpiredRefreshToken() {
        String expiredToken = createToken(secretKey, "Refresh", expiredIssuedAt(), expiredExpiration());

        assertThat(jwtProvider.isExpiredAccessToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isExpiredAccessToken - token_type이 없는 기존 토큰이 만료되면 false")
    void isExpiredAccessToken_false_whenNoTokenType() {
        String expiredToken = createToken(secretKey, null, expiredIssuedAt(), expiredExpiration());

        assertThat(jwtProvider.isExpiredAccessToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isExpiredAccessToken - 서명이 위조된 토큰이면 false (만료가 아니라 무효라서 재발급 대상 아님)")
    void isExpiredAccessToken_false_whenTampered() {
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String tamperedToken = createToken(wrongKey, "Access", new Date(), new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertThat(jwtProvider.isExpiredAccessToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("isAccessToken / isRefreshToken - 생성 메서드에 따라 토큰 종류가 구분됨")
    void tokenType_distinguished() {
        String accessToken = jwtProvider.generateAccessToken(1L, "USER");
        String refreshToken = jwtProvider.generateRefreshToken(1L);

        assertThat(jwtProvider.isAccessToken(jwtProvider.parseClaims(accessToken))).isTrue();
        assertThat(jwtProvider.isRefreshToken(jwtProvider.parseClaims(accessToken))).isFalse();
        assertThat(jwtProvider.isRefreshToken(jwtProvider.parseClaims(refreshToken))).isTrue();
        assertThat(jwtProvider.isAccessToken(jwtProvider.parseClaims(refreshToken))).isFalse();
    }

    @Test
    @DisplayName("isAccessToken / isRefreshToken - token_type이 없는 기존 토큰은 둘 다 false")
    void tokenType_noTokenType() {
        String token = createToken(secretKey, null, new Date(), new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertThat(jwtProvider.isAccessToken(jwtProvider.parseClaims(token))).isFalse();
        assertThat(jwtProvider.isRefreshToken(jwtProvider.parseClaims(token))).isFalse();
    }

    private Date expiredIssuedAt() {
        return new Date(System.currentTimeMillis() - 1000 * 60 * 60); // 1시간 전 발급
    }

    private Date expiredExpiration() {
        return new Date(System.currentTimeMillis() - 1000 * 60); // 1분 전 만료
    }

    // tokenType이 null이면 token_type 클레임 없이 생성 (배포 전 발급된 토큰 재현)
    private String createToken(SecretKey key, String tokenType, Date issuedAt, Date expiration) {
        JwtBuilder builder = Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key);
        if (tokenType != null) {
            builder.claim("token_type", tokenType);
        }
        return builder.compact();
    }
}
