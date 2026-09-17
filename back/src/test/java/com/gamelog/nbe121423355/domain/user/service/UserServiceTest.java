package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.TokenResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User saveUser(String email, String nickname, String rawPassword) {
        return userRepository.save(
                new User(nickname, email, passwordEncoder.encode(rawPassword))
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("nickname", "test@test.com", "password123");

        UserDto userDto = userService.signUp(signupRequestDto);

        assertThat(userDto.email()).isEqualTo("test@test.com");
        assertThat(userDto.nickname()).isEqualTo("nickname");
        User saved = userRepository.findByEmail("test@test.com").orElseThrow();
        assertThat(saved.getId()).isEqualTo(userDto.id());
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_fail_duplicateEmail() {
        saveUser("test@test.com", "other", "password123");
        SignupRequestDto signupRequestDto = new SignupRequestDto("nickname", "test@test.com", "password123");

        assertThatThrownBy(() -> userService.signUp(signupRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("409-1"));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signUp_fail_duplicateNickname() {
        saveUser("other@test.com", "nickname", "password123");
        SignupRequestDto signupRequestDto = new SignupRequestDto("nickname", "test@test.com", "password123");

        assertThatThrownBy(() -> userService.signUp(signupRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("409-2"));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");

        UserService.LoginResult result = userService.login(loginRequestDto);

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("로그인 성공 - accessToken 클레임에 userId/role만 있고 email/nickname은 없음")
    void login_accessTokenClaims_excludeEmailAndNickname() {
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");

        UserService.LoginResult result = userService.login(loginRequestDto);
        Claims claims = jwtProvider.parseClaims(result.accessToken());

        assertThat(claims.get("email")).isNull();
        assertThat(claims.get("nickname")).isNull();
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(Long.parseLong(claims.getSubject())).isEqualTo(result.user().id());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_fail_emailNotFound() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("unknown@test.com", "password123");

        assertThatThrownBy(() -> userService.login(loginRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-1"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() {
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "wrongPassword");

        assertThatThrownBy(() -> userService.login(loginRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-1"));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success() {
        User user = saveUser("test@test.com", "nickname", "password123");
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        TokenResponseDto tokenResponseDto = userService.refresh(refreshToken);

        assertThat(tokenResponseDto.accessToken()).isNotBlank();
        assertThat(jwtProvider.getUserId(tokenResponseDto.accessToken())).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refresh_fail_invalidToken() {
        assertThatThrownBy(() -> userService.refresh("invalid-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰은 유효하나 존재하지 않는 유저")
    void refresh_fail_userNotFound() {
        String refreshToken = jwtProvider.generateRefreshToken(999_999L);

        assertThatThrownBy(() -> userService.refresh(refreshToken))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    @Test
    @DisplayName("유저 정보 호출 - 성공")
    void getMe_success() {
        User user = saveUser("test@test.com", "nickname", "password123");

        UserDto userDto = userService.getMe(user.getId());

        assertThat(userDto.id()).isEqualTo(user.getId());
        assertThat(userDto.email()).isEqualTo("test@test.com");
        assertThat(userDto.nickname()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("유저 정보 호출 - 실패 (존재하지 않는 유저)")
    void getMe_fail_userNotFound() {
        assertThatThrownBy(() -> userService.getMe(999_999L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }

    @Test
    @DisplayName("온보딩 완료 성공")
    void exitOnboarding_success() {
        User user = saveUser("test@test.com", "nickname", "password123");

        UserDto userDto = userService.exitOnboarding(user.getId());

        assertThat(userDto.id()).isEqualTo(user.getId());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("온보딩 완료 실패 - 존재하지 않는 유저")
    void exitOnboarding_fail_userNotFound() {
        assertThatThrownBy(() -> userService.exitOnboarding(999_999L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }

    @Test
    @DisplayName("온보딩 스킵 성공")
    void skipOnboarding_success() {
        User user = saveUser("test@test.com", "nickname", "password123");

        UserDto userDto = userService.skipOnboarding(user.getId());

        assertThat(userDto.id()).isEqualTo(user.getId());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("온보딩 스킵 실패 - 존재하지 않는 유저")
    void skipOnboarding_fail_userNotFound() {
        assertThatThrownBy(() -> userService.skipOnboarding(999_999L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }
}
