package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiV1UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private GameRepository gameRepository;

    private User saveUser(String email, String nickname, String rawPassword) {
        return userRepository.save(new User(nickname, email, passwordEncoder.encode(rawPassword)));
    }

    private String accessTokenFor(User user) {
        return jwtProvider.generateAccessToken(user.getId(), user.getRole());
    }

    private Genre saveGenre(String name) {
        return genreRepository.save(Genre.createFromIgdb(null, name));
    }

    private Game saveGame(String title) {
        IgdbGameResponse response = new IgdbGameResponse(
                (long) title.hashCode(),
                title,
                "테스트 게임 설명",
                null,
                1704067200L,
                new BigDecimal("90.50"),
                List.of(), List.of(), List.of(), List.of()
        );
        return gameRepository.save(Game.createFromIgdb(response));
    }

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "테스트유저", "email": "test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 (빈 닉네임)")
    void signUp_fail_validation() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "", "email": "test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_fail_duplicateEmail() throws Exception {
        saveUser("test@test.com", "other", "password123");

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "테스트유저", "email": "test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signUp_fail_duplicateNickname() throws Exception {
        saveUser("other@test.com", "테스트유저", "password123");

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "테스트유저", "email": "test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-2"));
    }

    @Test
    @DisplayName("로그인 성공 - accessToken 응답 + refreshToken 쿠키")
    void login_success() throws Exception {
        saveUser("test@test.com", "nickname", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@test.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        Cookie refreshToken = result.getResponse().getCookie("refreshToken");
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() throws Exception {
        saveUser("test@test.com", "nickname", "password123");

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@test.com", "password": "wrongPassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        mockMvc.perform(post("/api/v1/users/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("내 정보 조회 - 인증 없이 호출하면 401")
    void getMe_fail_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessTokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-3"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    @DisplayName("온보딩 완료 성공")
    void onboarding_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");

        mockMvc.perform(patch("/api/v1/users/me/onboarding")
                        .header("Authorization", "Bearer " + accessTokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-4"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("온보딩 스킵 성공")
    void onboardingSkip_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");

        mockMvc.perform(patch("/api/v1/users/me/onboarding/skip")
                        .header("Authorization", "Bearer " + accessTokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-5"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("선호 장르 저장 성공")
    void updatePreferredGenres_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        Genre action = saveGenre("액션");
        Genre rpg = saveGenre("RPG");

        mockMvc.perform(put("/api/v1/users/me/preferred-genres")
                        .header("Authorization", "Bearer " + accessTokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"genreIds\": [%d, %d]}".formatted(action.getId(), rpg.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-6"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("선호 장르 저장 실패 - 최대 3개 초과")
    void updatePreferredGenres_fail_tooMany() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        Long g1 = saveGenre("액션").getId();
        Long g2 = saveGenre("RPG").getId();
        Long g3 = saveGenre("어드벤처").getId();
        Long g4 = saveGenre("퍼즐").getId();

        mockMvc.perform(put("/api/v1/users/me/preferred-genres")
                        .header("Authorization", "Bearer " + accessTokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"genreIds\": [%d, %d, %d, %d]}".formatted(g1, g2, g3, g4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("선호 장르 저장 - 인증 없이 호출하면 401")
    void updatePreferredGenres_fail_unauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/preferred-genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"genreIds\": [1]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("선호 장르 조회 성공")
    void getPreferredGenres_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        Genre action = saveGenre("액션");

        mockMvc.perform(put("/api/v1/users/me/preferred-genres")
                .header("Authorization", "Bearer " + accessTokenFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"genreIds\": [%d]}".formatted(action.getId())));

        mockMvc.perform(get("/api/v1/users/me/preferred-genres")
                        .header("Authorization", "Bearer " + accessTokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-7"))
                .andExpect(jsonPath("$.data[0].genreName").value("액션"));
    }

    @Test
    @DisplayName("선호 게임 저장 성공")
    void updatePreferredGames_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        Game witcher = saveGame("The Witcher 3");

        mockMvc.perform(put("/api/v1/users/me/preferred-games")
                        .header("Authorization", "Bearer " + accessTokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameIds\": [%d]}".formatted(witcher.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-8"))
                .andExpect(jsonPath("$.data[0].gameTitle").value("The Witcher 3"));
    }

    @Test
    @DisplayName("선호 게임 조회 성공")
    void getPreferredGames_success() throws Exception {
        User user = saveUser("test@test.com", "nickname", "password123");
        Game witcher = saveGame("The Witcher 3");

        mockMvc.perform(put("/api/v1/users/me/preferred-games")
                .header("Authorization", "Bearer " + accessTokenFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameIds\": [%d]}".formatted(witcher.getId())));

        mockMvc.perform(get("/api/v1/users/me/preferred-games")
                        .header("Authorization", "Bearer " + accessTokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-9"))
                .andExpect(jsonPath("$.data[0].gameTitle").value("The Witcher 3"));
    }
}
