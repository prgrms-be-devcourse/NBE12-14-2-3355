package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
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
class TokenReissueServiceTest {

    @Autowired
    private TokenReissueService tokenReissueService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(nickname, email, passwordEncoder.encode("password123")));
    }

    private String loginAndGetRefreshToken(String email) {
        return userService.login(new LoginRequestDto(email, "password123")).refreshToken();
    }

    @Test
    @DisplayName("재발급 성공 - expectedUserId가 refreshToken의 사용자와 같으면 해당 사용자의 accessToken 발급")
    void reissue_success_userIdMatch() {
        User user = saveUser("test@test.com", "nickname");
        String refreshToken = loginAndGetRefreshToken("test@test.com");

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(refreshToken, user.getId());

        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.role()).isEqualTo(user.getRole());
        assertThat(jwtProvider.getUserId(result.accessToken())).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("재발급 성공 - expectedUserId 없이(/refresh API) 호출하면 비교 없이 발급")
    void reissue_success_withoutExpectedUserId() {
        User user = saveUser("test@test.com", "nickname");
        String refreshToken = loginAndGetRefreshToken("test@test.com");

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(refreshToken);

        assertThat(result.userId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("재발급 실패 - expectedUserId가 refreshToken의 사용자와 다르면 401-2")
    void reissue_fail_userIdMismatch() {
        User userA = saveUser("a@test.com", "userA");
        saveUser("b@test.com", "userB");
        String refreshTokenOfB = loginAndGetRefreshToken("b@test.com");

        assertThatThrownBy(() -> tokenReissueService.reissue(refreshTokenOfB, userA.getId()))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }
}
