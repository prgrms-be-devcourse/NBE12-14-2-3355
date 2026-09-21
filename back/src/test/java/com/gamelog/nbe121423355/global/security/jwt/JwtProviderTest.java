package com.gamelog.nbe121423355.global.security.jwt;

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
    @DisplayName("isExpired - 아직 만료되지 않은 토큰이면 false")
    void isExpired_false_whenNotExpired() {
        String token = jwtProvider.generateAccessToken(1L, "USER");

        assertThat(jwtProvider.isExpired(token)).isFalse();
    }

    @Test
    @DisplayName("isExpired - 시간이 지나 만료된 토큰이면 true")
    void isExpired_true_whenExpired() {
        String expiredToken = createToken(
                secretKey,
                1L,
                "USER",
                new Date(System.currentTimeMillis() - 1000 * 60 * 60), // 1시간 전 발급
                new Date(System.currentTimeMillis() - 1000 * 60)       // 1분 전 만료
        );

        assertThat(jwtProvider.isExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("isExpired - 서명이 위조된 토큰이면 false (만료가 아니라 무효라서 재발급 대상 아님)")
    void isExpired_false_whenTampered() {
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String tamperedToken = createToken(
                wrongKey,
                1L,
                "USER",
                new Date(),
                new Date(System.currentTimeMillis() + 1000 * 60 * 60)
        );

        assertThat(jwtProvider.isExpired(tamperedToken)).isFalse();
    }

    private String createToken(SecretKey key, Long userId, String role, Date issuedAt, Date expiration) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
