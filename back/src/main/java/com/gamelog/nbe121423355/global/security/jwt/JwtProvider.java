package com.gamelog.nbe121423355.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtProvider {

    // 토큰 종류 구분용 클레임
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "Access";
    private static final String REFRESH = "Refresh";
    // refreshToken의 로그인 세션(family) 식별 클레임
    private static final String FAMILY_ID = "fid";

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
                .claim(TOKEN_TYPE, ACCESS)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    // refreshtoken 생성 메소드
    // jti/만료시각을 밖에서 받는 이유: 유예 시간 내 재요청 시 같은 jti·같은 만료시각으로 다시 서명해야 함
    public String generateRefreshToken(Long userId, String familyId, String jti, Date expiration){
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .claim(TOKEN_TYPE, REFRESH)
                .claim(FAMILY_ID, familyId)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // 새 로그인 세션의 만료 시각 (지금 + refreshToken 유효기간)
    public Date newRefreshTokenExpiration() {
        return new Date(System.currentTimeMillis() + jwtProperties.refreshTokenValidity());
    }

    // refreshToken의 세션 식별자 (fid가 없는 기존 토큰은 null)
    public String getFamilyId(Claims claims) {
        return claims.get(FAMILY_ID, String.class);
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

    // 서명은 유효하지만 시간이 지나 만료된 accessToken이면 그 클레임을 반환 (자동 재발급 대상 여부 + 사용자 ID 비교용)
    // 만료되지 않았거나, accessToken이 아니거나, 서명이 위조된 토큰이면 empty
    public Optional<Claims> getExpiredAccessTokenClaims(String token) {
        try {
            parseClaims(token);
            return Optional.empty();
        } catch (ExpiredJwtException e) {
            return isAccessToken(e.getClaims()) ? Optional.of(e.getClaims()) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // 토큰 종류 판별 (token_type 클레임이 없는 기존 토큰은 false)
    public boolean isAccessToken(Claims claims) {
        return ACCESS.equals(claims.get(TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH.equals(claims.get(TOKEN_TYPE, String.class));
    }
}
