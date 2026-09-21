package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.TokenResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.UpdateProfileRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.UserDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import com.gamelog.nbe121423355.global.upload.ImageUploadService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private ImageUploadService imageUploadService;

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
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");
        UserService.LoginResult loginResult = userService.login(loginRequestDto);

        TokenResponseDto tokenResponseDto = userService.refresh(loginResult.refreshToken());

        assertThat(tokenResponseDto.accessToken()).isNotBlank();
        assertThat(jwtProvider.getUserId(tokenResponseDto.accessToken())).isEqualTo(loginResult.user().id());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refresh_fail_invalidToken() {
        assertThatThrownBy(() -> userService.refresh("invalid-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - RefreshToken DB에 없는 토큰(로그아웃되었거나 직접 생성된 토큰)")
    void refresh_fail_userNotFound() {
        String refreshToken = jwtProvider.generateRefreshToken(999_999L);

        assertThatThrownBy(() -> userService.refresh(refreshToken))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    @Test
    @DisplayName("로그아웃 성공 - RefreshToken row가 삭제되고, 이후 같은 토큰으로 재발급 시도하면 실패함")
    void logout_success() {
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");
        UserService.LoginResult loginResult = userService.login(loginRequestDto);
        String jti = jwtProvider.parseClaims(loginResult.refreshToken()).getId();
        assertThat(refreshTokenRepository.findByTokenId(jti)).isPresent();

        userService.logout(loginResult.refreshToken());

        assertThat(refreshTokenRepository.findByTokenId(jti)).isEmpty();
        assertThatThrownBy(() -> userService.refresh(loginResult.refreshToken()))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    @Test
    @DisplayName("로그아웃 - 이미 무효하거나 형식이 잘못된 토큰이어도 예외 없이 통과")
    void logout_invalidToken_noException() {
        assertDoesNotThrow(() -> userService.logout("invalid-token"));
    }

    @Test
    @DisplayName("멀티 디바이스 - 같은 유저로 두 번 로그인하면 각각 별도 세션으로 관리되어, 한쪽 로그아웃이 다른 쪽에 영향 없음")
    void login_twice_independentSessions() {
        saveUser("test@test.com", "nickname", "password123");
        LoginRequestDto loginRequestDto = new LoginRequestDto("test@test.com", "password123");

        UserService.LoginResult device1 = userService.login(loginRequestDto);
        UserService.LoginResult device2 = userService.login(loginRequestDto);
        userService.logout(device1.refreshToken());

        assertThatThrownBy(() -> userService.refresh(device1.refreshToken()))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
        TokenResponseDto tokenResponseDto = userService.refresh(device2.refreshToken());
        assertThat(tokenResponseDto.accessToken()).isNotBlank();
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

    @Test
    @DisplayName("이메일 중복 확인 - 존재하는 이메일이면 true")
    void checkEmailDuplicate_true() {
        saveUser("test@test.com", "nickname", "password123");

        assertThat(userService.checkEmailDuplicate("test@test.com")).isTrue();
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않는 이메일이면 false")
    void checkEmailDuplicate_false() {
        assertThat(userService.checkEmailDuplicate("unknown@test.com")).isFalse();
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 존재하는 닉네임이면 true")
    void checkNicknameDuplicate_true() {
        saveUser("test@test.com", "nickname", "password123");

        assertThat(userService.checkNicknameDuplicate("nickname")).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 존재하지 않는 닉네임이면 false")
    void checkNicknameDuplicate_false() {
        assertThat(userService.checkNicknameDuplicate("unknown")).isFalse();
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() {
        User user = saveUser("test@test.com", "nickname", "password123");
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("newNickname", "http://image.com/a.png", "안녕하세요");

        UserDto userDto = userService.updateProfile(user.getId(), requestDto);

        assertThat(userDto.nickname()).isEqualTo("newNickname");
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getNickname()).isEqualTo("newNickname");
        assertThat(updated.getProfileImageUrl()).isEqualTo("http://image.com/a.png");
        assertThat(updated.getBio()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임을 그대로 유지해도 중복 에러 안 남")
    void updateProfile_success_sameNickname() {
        User user = saveUser("test@test.com", "nickname", "password123");
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("nickname", null, "안녕하세요");

        UserDto userDto = userService.updateProfile(user.getId(), requestDto);

        assertThat(userDto.nickname()).isEqualTo("nickname");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getBio()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("프로필 수정 실패 - 다른 유저의 닉네임과 중복")
    void updateProfile_fail_duplicateNickname() {
        saveUser("other@test.com", "takenNickname", "password123");
        User user = saveUser("test@test.com", "nickname", "password123");
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("takenNickname", null, null);

        assertThatThrownBy(() -> userService.updateProfile(user.getId(), requestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("409-2"));
    }

    @Test
    @DisplayName("프로필 수정 실패 - 존재하지 않는 유저")
    void updateProfile_fail_userNotFound() {
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("nickname", null, null);

        assertThatThrownBy(() -> userService.updateProfile(999_999L, requestDto))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }

    @Test
    @DisplayName("프로필 수정 - 이미지 URL이 바뀌면 옛날 이미지를 삭제함")
    void updateProfile_deletesOldImage_whenImageUrlChanges() {
        User user = saveUser("test@test.com", "nickname", "password123");
        user.updateProfile(user.getNickname(), "http://image.com/old.png", user.getBio());
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("nickname", "http://image.com/new.png", null);

        userService.updateProfile(user.getId(), requestDto);

        verify(imageUploadService).deleteImage("http://image.com/old.png");
    }

    @Test
    @DisplayName("프로필 수정 - 이미지 URL이 그대로면 삭제 호출 안 함")
    void updateProfile_doesNotDeleteImage_whenImageUrlUnchanged() {
        User user = saveUser("test@test.com", "nickname", "password123");
        user.updateProfile(user.getNickname(), "http://image.com/same.png", user.getBio());
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("nickname", "http://image.com/same.png", null);

        userService.updateProfile(user.getId(), requestDto);

        verify(imageUploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("프로필 수정 - 원래 이미지가 없었으면 삭제 호출 안 함")
    void updateProfile_doesNotDeleteImage_whenOldUrlWasNull() {
        User user = saveUser("test@test.com", "nickname", "password123");
        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("nickname", "http://image.com/new.png", null);

        userService.updateProfile(user.getId(), requestDto);

        verify(imageUploadService, never()).deleteImage(anyString());
    }
}
