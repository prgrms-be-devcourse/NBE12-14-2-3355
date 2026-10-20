package com.gamelog.nbe121423355.global.security.jwt;

import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtProvider jwtProvider;

    private User saveUser(String email, String nickname, String rawPassword) {
        return userRepository.save(new User(nickname, email, passwordEncoder.encode(rawPassword)));
    }

    private Cookie loginAndGetRefreshTokenCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "autorefresh-test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(refreshTokenCookie).isNotNull();
        return refreshTokenCookie;
    }

    // 시간이 지나 만료된(서명은 정상) accessToken을 직접 생성
    private String expiredAccessToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("token_type", "Access")
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60))
                .signWith(key)
                .compact();
    }

    // 시간이 지나 만료된(서명은 정상) refreshToken을 직접 생성
    private String expiredRefreshToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id("expired-refresh-jti")
                .claim("token_type", "Refresh")
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60))
                .signWith(key)
                .compact();
    }

    // 서명이 위조된(만료는 아닌) accessToken을 직접 생성
    private String tamperedAccessToken(Long userId, String role) {
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(wrongKey)
                .compact();
    }

    @Test
    @DisplayName("accessToken 만료 + 유효한 refreshToken 쿠키 → 자동 재발급 성공, New-Access-Token 헤더 포함")
    void autoReissue_success() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie();

        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredAccessToken(user.getId(), user.getRole()))
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(header().exists("New-Access-Token"))
                .andReturn();

        // Rotation: 자동 재발급 시에도 교체된 refreshToken이 Set-Cookie로 내려옴
        Cookie rotatedCookie = result.getResponse().getCookie("refreshToken");
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshTokenCookie.getValue());
    }

    @Test
    @DisplayName("accessToken 만료 + refreshToken 쿠키 없음 → 재발급 안 되고 401")
    void autoReissue_fail_noRefreshCookie() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredAccessToken(user.getId(), user.getRole())))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }

    @Test
    @DisplayName("accessToken 만료 + refreshToken이 DB에 없음(로그인 거치지 않고 직접 생성한 토큰) → 재발급 안 되고 401")
    void autoReissue_fail_refreshTokenNotInDb() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), jwtProvider.newRefreshTokenExpiration());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredAccessToken(user.getId(), user.getRole()))
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }

    @Test
    @DisplayName("accessToken 서명 위조(만료 아님) + 유효한 refreshToken 쿠키 → 재발급 시도 자체를 안 하고 401")
    void autoReissue_fail_tamperedAccessToken() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tamperedAccessToken(user.getId(), user.getRole()))
                        .cookie(refreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }

    @Test
    @DisplayName("유효한 refreshToken을 Authorization 헤더에 넣으면 → 인증되지 않고 401")
    void authenticate_fail_refreshTokenAsBearer() throws Exception {
        saveUser("autorefresh-test@test.com", "nickname", "password123");
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + refreshTokenCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 refreshToken을 Authorization 헤더에 넣으면 + 유효한 refreshToken 쿠키 → 재발급 시도 자체를 안 하고 401")
    void autoReissue_fail_expiredRefreshTokenAsBearer() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredRefreshToken(user.getId()))
                        .cookie(refreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }

    @Test
    @DisplayName("A의 만료된 accessToken + B의 refreshToken 쿠키(두 탭 다른 계정) → B 토큰으로 재발급되지 않고 401")
    void autoReissue_fail_userIdMismatch() throws Exception {
        User userA = saveUser("other-user@test.com", "userA", "password123");
        saveUser("autorefresh-test@test.com", "userB", "password123");
        Cookie refreshTokenCookieOfB = loginAndGetRefreshTokenCookie();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredAccessToken(userA.getId(), userA.getRole()))
                        .cookie(refreshTokenCookieOfB))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }

    @Test
    @DisplayName("accessToken 만료 + refreshToken 쿠키에 accessToken이 들어있음 → 재발급 안 되고 401")
    void autoReissue_fail_accessTokenInRefreshCookie() throws Exception {
        User user = saveUser("autorefresh-test@test.com", "nickname", "password123");
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredAccessToken(user.getId(), user.getRole()))
                        .cookie(new Cookie("refreshToken", accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("New-Access-Token"));
    }
}
