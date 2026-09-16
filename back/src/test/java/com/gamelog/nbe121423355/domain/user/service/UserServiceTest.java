package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.TokenResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("nickname", "test@test.com", "password123");
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class)))
                .willReturn(new User(1L, "test@test.com", "encodedPassword", "nickname", null, null, false, "USER"));

        UserDto userDto = userService.signUp(signupRequestDto);

        assertThat(userDto.id()).isEqualTo(1L);
        assertThat(userDto.email()).isEqualTo("test@test.com");
        assertThat(userDto.nickname()).isEqualTo("nickname");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_fail_duplicateEmail() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("nickname", "test@test.com", "password123");
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(signupRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("409-1"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        User user = new User(1L, "test@test.com", "encodedPassword", "nickname", null, null, false, "USER");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(1L, "test@test.com", "USER")).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        UserService.LoginResult result = userService.login(loginRequestDto);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().email()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_fail_emailNotFound() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("unknown@test.com", "password123");
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-1"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() {
        User user = new User(1L, "test@test.com", "encodedPassword", "nickname", null, null, false, "USER");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "wrongPassword");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-1"));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success() {
        User user = new User(1L, "test@test.com", "encodedPassword", "nickname", null, null, false, "USER");
        given(jwtProvider.validateToken("valid-refresh-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-refresh-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(1L, "test@test.com", "USER")).willReturn("new-access-token");

        TokenResponseDto tokenResponseDto = userService.refresh("valid-refresh-token");

        assertThat(tokenResponseDto.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refresh_fail_invalidToken() {
        given(jwtProvider.validateToken("invalid-token")).willReturn(false);

        assertThatThrownBy(() -> userService.refresh("invalid-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));

        verify(jwtProvider, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰은 유효하나 존재하지 않는 유저")
    void refresh_fail_userNotFound() {
        given(jwtProvider.validateToken("valid-refresh-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-refresh-token")).willReturn(999L);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh("valid-refresh-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }
}
