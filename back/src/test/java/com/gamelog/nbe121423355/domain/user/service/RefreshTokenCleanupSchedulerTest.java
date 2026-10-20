package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenCleanupSchedulerTest {

    @Autowired
    private RefreshTokenCleanupScheduler refreshTokenCleanupScheduler;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("만료된 RefreshToken만 삭제되고, 아직 유효한 RefreshToken은 남음")
    void deleteExpiredRefreshTokens_onlyExpired() {
        User user = userRepository.save(new User("nickname", "test@test.com", "encoded-password"));
        refreshTokenRepository.save(RefreshToken.create(user, "expired-family", "expired-jti", LocalDateTime.now().minusDays(1)));
        refreshTokenRepository.save(RefreshToken.create(user, "valid-family", "valid-jti", LocalDateTime.now().plusDays(1)));

        refreshTokenCleanupScheduler.deleteExpiredRefreshTokens();

        assertThat(refreshTokenRepository.findByFamilyId("expired-family")).isEmpty();
        assertThat(refreshTokenRepository.findByFamilyId("valid-family")).isPresent();
    }
}
