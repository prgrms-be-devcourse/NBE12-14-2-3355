package com.gamelog.nbe121423355.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    // 토큰 제공 클래스
    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] KeyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(KeyBytes);
    }

    // accesstoken 생성 메소드
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.accessTokenValidity());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    // refreshtoken 생성 메소드
    public String generateRefreshToken(Long userId){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.refreshTokenValidity());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // 클레임에서 userId 추출
    public Long getUserId(String token){
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 토큰의 클레임 전체 파싱 (서명 검증 포함)
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 서명/만료 검증
    public boolean validateToken(String token){
        try{
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build() // 시크릿키로 서명 검증
                    .parseSignedClaims(token); // 토큰을 파싱하면서 서명검증까지 수행, 서명이 안맞거나 만료되었으면 여기서 예외를 던지기
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
